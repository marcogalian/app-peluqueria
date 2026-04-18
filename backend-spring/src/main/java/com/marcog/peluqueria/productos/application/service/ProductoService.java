package com.marcog.peluqueria.productos.application.service;

import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.in.GestionarProductoUseCase;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductoService implements GestionarProductoUseCase {

    private final ProductoRepositoryPort repository;
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
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
        int nuevoStock = Math.max(0, p.getStock() + cantidad);
        p.setStock(nuevoStock);
        Producto guardado = repository.guardar(p);

        if (p.getStockMinimo() != null && nuevoStock <= p.getStockMinimo()) {
            notificationService.notificarAdmin(
                    "⚠️ Stock bajo: " + p.getNombre(),
                    "El producto \"" + p.getNombre() + "\" tiene stock bajo.\n" +
                    "Stock actual: " + nuevoStock + " unidades (mínimo: " + p.getStockMinimo() + ").\n\n" +
                    "Por favor, realiza un pedido a tu proveedor.\n\n" +
                    "Peluquería Isabella"
            );
        }

        return guardado;
    }

    @Override
    public List<Producto> listar(CategoriaProducto categoria) {
        if (categoria != null) return repository.findByCategoria(categoria);
        return repository.findAll();
    }
}
