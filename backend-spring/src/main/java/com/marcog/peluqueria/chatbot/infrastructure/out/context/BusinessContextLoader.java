package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.servicios.domain.port.out.ServicioRepository;
import com.marcog.peluqueria.servicios.domain.model.Servicio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessContextLoader {

    private final ServicioRepository servicioRepository;
    private final ProductoRepositoryPort productoRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String cachedContext = "";

    @PostConstruct
    public void init() {
        regenerar();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void regenerar() {
        try {
            ObjectNode root = mapper.createObjectNode();

            root.put("negocio", "Peluqueria Isabella");
            root.put("horario", "Lunes a Viernes 9:00-21:00, Sabados 9:00-14:00, Domingos cerrado");
            root.put("direccion", "Calle Principal 15, Madrid");
            root.put("telefono", "+34 600 123 456");

            // Servicios
            List<Servicio> servicios = servicioRepository.findAll();
            ArrayNode serviciosNode = mapper.createArrayNode();
            for (Servicio s : servicios) {
                ObjectNode sn = mapper.createObjectNode();
                sn.put("nombre", s.getNombre());
                sn.put("precio", s.getPrecio() != null ? s.getPrecio().doubleValue() : 0);
                sn.put("duracionMinutos", s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 0);
                if (s.getCategoria() != null) sn.put("categoria", s.getCategoria().name());
                if (s.getGenero() != null) sn.put("genero", s.getGenero().name());
                serviciosNode.add(sn);
            }
            root.set("servicios", serviciosNode);

            // Productos
            List<Producto> productos = productoRepository.findAll();
            ArrayNode productosNode = mapper.createArrayNode();
            for (Producto p : productos) {
                if (p.getActivo() == null || !p.getActivo()) continue;
                ObjectNode pn = mapper.createObjectNode();
                pn.put("nombre", p.getNombre());
                pn.put("precio", p.getPrecio() != null ? p.getPrecio().doubleValue() : 0);
                if (p.getCategoria() != null) pn.put("categoria", p.getCategoria().name());
                pn.put("stock", p.getStock() != null ? p.getStock() : 0);
                productosNode.add(pn);
            }
            root.set("productos", productosNode);

            // Politicas
            ObjectNode politicas = mapper.createObjectNode();
            politicas.put("cancelacion", "Se puede cancelar hasta 24h antes sin coste");
            politicas.put("vacaciones", "22 dias laborables al anio, solicitar con 15 dias de antelacion");
            politicas.put("descuentoVip", "Los clientes VIP tienen descuento personalizado por cliente");
            root.set("politicas", politicas);

            cachedContext = mapper.writeValueAsString(root);
            log.info("Contexto de negocio regenerado: {} servicios, {} productos", servicios.size(), productos.size());
        } catch (Exception e) {
            log.error("Error regenerando contexto de negocio: {}", e.getMessage());
        }
    }

    public String getContext() {
        return cachedContext;
    }
}
