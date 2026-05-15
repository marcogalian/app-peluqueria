package com.marcog.peluqueria.productos.application;

import com.marcog.peluqueria.productos.application.dto.ResumenVentasProductosDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaRequestDTO;
import com.marcog.peluqueria.productos.application.dto.VentaAgrupadaResponseDTO;
import com.marcog.peluqueria.productos.application.dto.VentaProductoResponseDTO;
import com.marcog.peluqueria.productos.domain.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.Producto;

import java.util.List;
import java.util.UUID;

public interface GestionarProducto {
    Producto crear(Producto producto);
    Producto actualizar(UUID id, Producto producto);
    void eliminar(UUID id);
    Producto ajustarStock(UUID id, int cantidad);
    VentaProductoResponseDTO vender(UUID id, int cantidad);
    VentaAgrupadaResponseDTO venderAgrupado(VentaAgrupadaRequestDTO request, String username);
    ResumenVentasProductosDTO obtenerResumenVentas();
    List<Producto> listar(CategoriaProducto categoria);
}
