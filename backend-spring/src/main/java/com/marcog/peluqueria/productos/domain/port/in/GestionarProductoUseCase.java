package com.marcog.peluqueria.productos.domain.port.in;

import com.marcog.peluqueria.productos.application.dto.ResumenVentasProductosDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoResponseDTO;
import com.marcog.peluqueria.productos.domain.model.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.model.Producto;

import java.util.List;
import java.util.UUID;

public interface GestionarProductoUseCase {
    Producto crear(Producto producto);
    Producto actualizar(UUID id, Producto producto);
    void eliminar(UUID id);
    Producto ajustarStock(UUID id, int cantidad);
    VentaProductoResponseDTO vender(UUID id, int cantidad);
    ResumenVentasProductosDTO obtenerResumenVentas();
    List<Producto> listar(CategoriaProducto categoria);
}
