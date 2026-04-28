package com.marcog.peluqueria.productos.application.service;

import com.marcog.peluqueria.productos.application.dto.ResumenVentasProductosDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoResponseDTO;
import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.in.GestionarProductoUseCase;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.JpaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.JpaVentaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.out.persistence.VentaProductoEntity;
import com.marcog.peluqueria.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProductoService implements GestionarProductoUseCase {

    private final ProductoRepositoryPort repository;
    private final JpaProductoRepository jpaProductoRepository;
    private final JpaVentaProductoRepository ventaProductoRepository;
    private final NotificationService notificationService;

    @Override
    public Producto crear(Producto producto) {
        producto.setActivo(true);
        return repository.guardar(producto);
    }

    @Override
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
    public void eliminar(UUID id) {
        Producto p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        p.setActivo(false);
        repository.guardar(p);
    }

    @Override
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

        var productoEntity = jpaProductoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Producto no encontrado: " + id));

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
            notificationService.notificarAdmin(
                    "⚠️ Stock bajo: " + p.getNombre(),
                    "El producto \"" + p.getNombre() + "\" tiene stock bajo.\n" +
                    "Stock actual: " + p.getStock() + " unidades (mínimo: " + p.getStockMinimo() + ").\n\n" +
                    "Por favor, realiza un pedido a tu proveedor.\n\n" +
                    "Peluquería Isabella"
            );
        }
    }

    @Override
    public List<Producto> listar(CategoriaProducto categoria) {
        if (categoria != null) return repository.findByCategoria(categoria);
        return repository.findAll();
    }
}
