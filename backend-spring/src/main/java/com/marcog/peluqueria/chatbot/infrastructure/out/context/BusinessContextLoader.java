package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.productos.domain.model.Producto;
import com.marcog.peluqueria.productos.domain.port.out.ProductoRepositoryPort;
import com.marcog.peluqueria.servicios.domain.model.Servicio;
import com.marcog.peluqueria.servicios.domain.port.out.ServicioRepository;
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
    @Scheduled(cron = "0 0 3 * * *")
    public void regenerar() {
        try {
            ObjectNode root = mapper.createObjectNode();

            // Datos estáticos del negocio
            ObjectNode negocio = mapper.createObjectNode();
            negocio.put("nombre", "Peluquería Isabella");
            negocio.put("direccion", "Calle Principal 15, Madrid");
            negocio.put("telefono", "+34 600 123 456");
            negocio.put("email", "admin@peluqueria.com");
            ObjectNode horario = mapper.createObjectNode();
            horario.put("lunesViernes", "09:00 - 21:00");
            horario.put("sabado", "09:00 - 14:00");
            horario.put("domingo", "Cerrado");
            negocio.set("horario", horario);
            root.set("negocio", negocio);

            ObjectNode politicas = mapper.createObjectNode();
            politicas.put("cancelacion", "Se puede cancelar hasta 24 horas antes sin coste. Cancelaciones tardías pueden suponer un cargo del 50%.");
            politicas.put("reservas", "Las citas se pueden reservar por teléfono o a través de la aplicación.");
            politicas.put("vacacionesEmpleados", "22 días laborables al año, solicitar con 15 días de antelación mínimo.");
            politicas.put("descuentoVip", "Los clientes VIP tienen descuento personalizado según su historial.");
            politicas.put("formasPago", "Efectivo, tarjeta de crédito/débito y transferencia bancaria.");
            root.set("politicas", politicas);

            // Servicios desde BD
            List<Servicio> servicios = servicioRepository.findAll();
            ArrayNode serviciosNode = mapper.createArrayNode();
            for (Servicio s : servicios) {
                ObjectNode sn = mapper.createObjectNode();
                sn.put("nombre", s.getNombre());
                sn.put("precio", s.getPrecio() != null ? s.getPrecio().doubleValue() : 0);
                sn.put("duracionMinutos", s.getDuracionMinutos() != null ? s.getDuracionMinutos() : 0);
                if (s.getGenero() != null) sn.put("genero", s.getGenero().name());
                if (s.getCategoria() != null) sn.put("categoria", s.getCategoria().name());
                if (s.getDescripcion() != null) sn.put("descripcion", s.getDescripcion());
                serviciosNode.add(sn);
            }
            root.set("servicios", serviciosNode);

            // Productos activos desde BD
            List<Producto> productos = productoRepository.findAll();
            ArrayNode productosNode = mapper.createArrayNode();
            for (Producto p : productos) {
                if (!p.isActivo()) continue;
                ObjectNode pn = mapper.createObjectNode();
                pn.put("nombre", p.getNombre());
                pn.put("precio", p.getPrecio() != null ? p.getPrecio().doubleValue() : 0);
                if (p.getCategoria() != null) pn.put("categoria", p.getCategoria().name());
                if (p.getGenero() != null) pn.put("genero", p.getGenero().name());
                productosNode.add(pn);
            }
            root.set("productos", productosNode);

            cachedContext = mapper.writeValueAsString(root);
            log.info("Contexto regenerado desde BD: {} servicios, {} productos activos",
                    servicios.size(), productosNode.size());
        } catch (Exception e) {
            log.error("Error regenerando contexto: {}", e.getMessage());
        }
    }

    public String getContext() {
        return cachedContext;
    }
}
