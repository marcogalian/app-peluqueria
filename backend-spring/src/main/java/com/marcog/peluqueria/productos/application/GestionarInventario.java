package com.marcog.peluqueria.productos.application;

import com.marcog.peluqueria.productos.application.dto.ResumenVentasProductosDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaRequestDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaResponseDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoResponseDTO;
import com.marcog.peluqueria.productos.domain.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.Producto;
import com.marcog.peluqueria.productos.application.GestionarProducto;
import com.marcog.peluqueria.productos.domain.ProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaVentaRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.VentaEntity;
import com.marcog.peluqueria.productos.infrastructure.persistence.VentaProductoEntity;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.shared.notification.Notificaciones;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class GestionarInventario implements GestionarProducto {

    private final ProductoRepository repository;
    private final JpaProductoRepository jpaProductoRepository;
    private final JpaVentaProductoRepository ventaProductoRepository;
    private final JpaVentaRepository ventaRepository;
    private final JpaUserRepository userRepository;
    private final JpaPeluqueroRepository peluqueroRepository;
    private final Notificaciones notificaciones;

    @Override
    @CacheEvict(value = "productos", allEntries = true)
    public Producto crear(Producto producto) {
        producto.setActivo(true);
        return repository.guardar(producto);
    }

    @Override
    @CacheEvict(value = "productos", allEntries = true)
    public Producto actualizar(UUID id, Producto detalles) {
        Producto existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        if (detalles.getNombre() != null)          existente.setNombre(detalles.getNombre());
        if (detalles.getDescripcion() != null)     existente.setDescripcion(detalles.getDescripcion());
        if (detalles.getPrecio() != null)          existente.setPrecio(detalles.getPrecio());
        if (detalles.getPrecioDescuento() != null) existente.setPrecioDescuento(detalles.getPrecioDescuento());
        if (detalles.getStock() != null)           existente.setStock(detalles.getStock());
        if (detalles.getStockMinimo() != null)     existente.setStockMinimo(detalles.getStockMinimo());
        if (detalles.getCategoria() != null)       existente.setCategoria(detalles.getCategoria());
        if (detalles.getImageUrl() != null)        existente.setImageUrl(detalles.getImageUrl());
        return repository.guardar(existente);
    }

    @Override
    @CacheEvict(value = "productos", allEntries = true)
    public void eliminar(UUID id) {
        Producto p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(false);
        repository.guardar(p);
    }

    @Override
    @CacheEvict(value = "productos", allEntries = true)
    public Producto ajustarStock(UUID id, int cantidad) {
        Producto p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado: " + id));
        int nuevoStock = Math.max(0, p.getStock() + cantidad);
        p.setStock(nuevoStock);
        Producto guardado = repository.guardar(p);

        notificarStockBajoSiHaceFalta(guardado);
        return guardado;
    }

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public VentaProductoResponseDTO vender(UUID id, int cantidad) {
        if (cantidad <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "La cantidad debe ser mayor que cero");
        }

        Producto p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado: " + id));

        int stockActual = p.getStock() != null ? p.getStock() : 0;
        if (stockActual < cantidad) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "No hay suficiente stock para vender " + cantidad + " unidades de " + p.getNombre()
            );
        }

        BigDecimal precioAplicado = obtenerPrecioVenta(p);
        BigDecimal totalVenta = precioAplicado.multiply(BigDecimal.valueOf(cantidad));

        p.setStock(stockActual - cantidad);
        Producto guardado = repository.guardar(p);

        // getReferenceById devuelve un proxy sin hacer SELECT extra (B5: evitar double lookup)
        var productoEntity = jpaProductoRepository.getReferenceById(id);

        VentaProductoEntity venta = ventaProductoRepository.save(VentaProductoEntity.builder()
                .producto(productoEntity)
                .productoNombre(guardado.getNombre())
                .cantidad(cantidad)
                .precioUnitario(precioAplicado)
                .total(totalVenta)
                .build());

        notificarStockBajoSiHaceFalta(guardado);

        return VentaProductoResponseDTO.builder()
                .producto(guardado)
                .venta(VentaProductoResponseDTO.Venta.builder()
                        .id(venta.getId())
                        .cantidad(venta.getCantidad())
                        .precioUnitario(venta.getPrecioUnitario())
                        .total(venta.getTotal())
                        .fechaVenta(venta.getFechaVenta())
                        .build())
                .resumen(obtenerResumenVentas())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "productos", allEntries = true)
    public VentaAgrupadaResponseDTO venderAgrupado(VentaAgrupadaRequestDTO request, String username) {
        if (request.getLineas() == null || request.getLineas().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "La venta debe tener al menos una línea");
        }

        UserEntity usuario = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuario no encontrado"));
        String vendedorNombre = peluqueroRepository.findByUserId(usuario.getId())
                .map(p -> p.getNombre())
                .orElse(usuario.getUsername());

        Map<UUID, Integer> cantidadesPorProducto = request.getLineas().stream()
                .collect(Collectors.groupingBy(
                        VentaAgrupadaRequestDTO.Linea::getProductoId,
                        Collectors.summingInt(VentaAgrupadaRequestDTO.Linea::getCantidad)
                ));

        List<Producto> productosActualizados = new ArrayList<>();
        List<VentaProductoEntity> lineasPendientes = new ArrayList<>();
        BigDecimal totalVenta = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : cantidadesPorProducto.entrySet()) {
            UUID productoId = entry.getKey();
            int cantidad = entry.getValue();
            if (cantidad <= 0) {
                throw new ResponseStatusException(BAD_REQUEST, "La cantidad debe ser mayor que cero");
            }

            Producto producto = repository.findById(productoId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado: " + productoId));
            int stockActual = producto.getStock() != null ? producto.getStock() : 0;
            if (stockActual < cantidad) {
                throw new ResponseStatusException(
                        BAD_REQUEST,
                        "No hay suficiente stock para vender " + cantidad + " unidades de " + producto.getNombre()
                );
            }

            BigDecimal precioAplicado = obtenerPrecioVenta(producto);
            BigDecimal totalLinea = precioAplicado.multiply(BigDecimal.valueOf(cantidad));
            totalVenta = totalVenta.add(totalLinea);

            producto.setStock(stockActual - cantidad);
            Producto guardado = repository.guardar(producto);
            productosActualizados.add(guardado);

            // Proxy lazy sin SELECT extra (B5: evitar double lookup en venta agrupada)
            var productoEntity = jpaProductoRepository.getReferenceById(productoId);

            lineasPendientes.add(VentaProductoEntity.builder()
                    .producto(productoEntity)
                    .productoNombre(guardado.getNombre())
                    .cantidad(cantidad)
                    .precioUnitario(precioAplicado)
                    .total(totalLinea)
                    .build());

            notificarStockBajoSiHaceFalta(guardado);
        }

        VentaEntity venta = ventaRepository.save(VentaEntity.builder()
                .numero(generarNumeroVenta())
                .usuario(usuario)
                .vendedorNombre(vendedorNombre)
                .metodoPago(request.getMetodoPago())
                .total(totalVenta)
                .build());

        List<VentaProductoEntity> lineasGuardadas = lineasPendientes.stream()
                .peek(linea -> linea.setVenta(venta))
                .map(ventaProductoRepository::save)
                .collect(Collectors.toList());

        return VentaAgrupadaResponseDTO.builder()
                .id(venta.getId())
                .numero(venta.getNumero())
                .vendedorNombre(venta.getVendedorNombre())
                .metodoPago(venta.getMetodoPago())
                .total(venta.getTotal())
                .fechaVenta(venta.getFechaVenta())
                .lineas(lineasGuardadas.stream()
                        .map(linea -> VentaAgrupadaResponseDTO.Linea.builder()
                                .id(linea.getId())
                                .productoId(linea.getProducto().getId())
                                .productoNombre(linea.getProductoNombre())
                                .cantidad(linea.getCantidad())
                                .precioUnitario(linea.getPrecioUnitario())
                                .total(linea.getTotal())
                                .build())
                        .collect(Collectors.toList()))
                .productosActualizados(productosActualizados)
                .resumen(obtenerResumenVentas())
                .build();
    }

    @Override
    public ResumenVentasProductosDTO obtenerResumenVentas() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioSemana = ahora.with(DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime inicioMes = ahora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime inicioAnio = ahora.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<VentaProductoEntity> ventasSemana = ventaProductoRepository.findByFechaVentaBetween(inicioSemana, ahora);
        List<VentaProductoEntity> ventasMes = ventaProductoRepository.findByFechaVentaBetween(inicioMes, ahora);
        List<VentaProductoEntity> ventasAnio = ventaProductoRepository.findByFechaVentaBetween(inicioAnio, ahora);

        return ResumenVentasProductosDTO.builder()
                .ingresosSemana(sumarTotales(ventasSemana))
                .ingresosMes(sumarTotales(ventasMes))
                .ingresosAnio(sumarTotales(ventasAnio))
                .unidadesSemana(sumarUnidades(ventasSemana))
                .unidadesMes(sumarUnidades(ventasMes))
                .unidadesAnio(sumarUnidades(ventasAnio))
                .build();
    }

    private BigDecimal obtenerPrecioVenta(Producto producto) {
        if (producto.getPrecioDescuento() != null && producto.getPrecioDescuento().compareTo(BigDecimal.ZERO) > 0) {
            return producto.getPrecioDescuento();
        }
        return producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO;
    }

    private String generarNumeroVenta() {
        // Antes: count + 1 -> race condition bajo concurrencia (dos peticiones leen el mismo count
        // y generan el mismo numero). Ahora: timestamp con milisegundos + sufijo random.
        // Probabilidad de colision practicamente nula sin necesidad de bloqueos en BD.
        String marcaTemporal = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS", Locale.ROOT));
        int sufijoAleatorio = java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);
        return "V-" + marcaTemporal + "-" + String.format("%03d", sufijoAleatorio);
    }

    private BigDecimal sumarTotales(List<VentaProductoEntity> ventas) {
        return ventas.stream()
                .map(VentaProductoEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int sumarUnidades(List<VentaProductoEntity> ventas) {
        return ventas.stream()
                .mapToInt(VentaProductoEntity::getCantidad)
                .sum();
    }

    private void notificarStockBajoSiHaceFalta(Producto p) {
        if (p.getStock() != null && p.getStockMinimo() != null && p.getStock() <= p.getStockMinimo()) {
            notificaciones.notificarAdmin(
                    "⚠️ Stock bajo: " + p.getNombre(),
                    "El producto \"" + p.getNombre() + "\" tiene stock bajo.\n" +
                    "Stock actual: " + p.getStock() + " unidades (mínimo: " + p.getStockMinimo() + ").\n\n" +
                    "Por favor, realiza un pedido a tu proveedor.\n\n" +
                    "Peluquería Isabella"
            );
        }
    }

    @Override
    @Cacheable("productos")
    public List<Producto> listar(CategoriaProducto categoria) {
        if (categoria != null) return repository.findByCategoria(categoria);
        return repository.findAll();
    }
}
