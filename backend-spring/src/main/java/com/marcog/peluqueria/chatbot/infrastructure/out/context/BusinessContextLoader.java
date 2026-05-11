package com.marcog.peluqueria.chatbot.infrastructure.out.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.ofertas.domain.model.Oferta;
import com.marcog.peluqueria.ofertas.domain.port.out.OfertaRepositoryPort;
import com.marcog.peluqueria.peluqueros.domain.model.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.port.out.PeluqueroRepositoryPort;
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

/**
 * Cachea en memoria el contexto estatico del negocio en formato JSON.
 *
 * Lo que entra aqui debe cambiar como mucho una vez al dia (servicios, productos,
 * politicas, equipo, ofertas). Los datos en tiempo real (citas, dinero, stock)
 * NO se cachean: van por function calling en ChatFunctionExecutor.
 *
 * Se regenera al arrancar la aplicacion y cada noche a las 03:00.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BusinessContextLoader {

    // ── Dependencias ────────────────────────────────────────────────
    private final ServicioRepository servicioRepository;
    private final ProductoRepositoryPort productoRepository;
    private final PeluqueroRepositoryPort peluqueroRepository;
    private final OfertaRepositoryPort ofertaRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String cachedContext = "";

    // ── Ciclo de vida ───────────────────────────────────────────────

    @PostConstruct
    @Scheduled(cron = "0 0 3 * * *")
    public void regenerar() {
        try {
            ObjectNode raiz = mapper.createObjectNode();

            raiz.set("negocio", construirNegocio());
            raiz.set("politicas", construirPoliticas());
            raiz.set("equipo", construirEquipo());
            raiz.put("totalEmpleados", peluqueroRepository.findAll().size());
            raiz.set("servicios", construirCatalogoServicios());
            raiz.set("productos", construirCatalogoProductos());
            raiz.set("ofertas", construirOfertasActivas());

            cachedContext = mapper.writeValueAsString(raiz);
            log.info("Contexto del chatbot regenerado correctamente");
        } catch (Exception ex) {
            log.error("Error regenerando contexto del chatbot: {}", ex.getMessage());
        }
    }

    public String getContext() {
        return cachedContext;
    }

    // ── Construccion del JSON ───────────────────────────────────────

    private ObjectNode construirNegocio() {
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

        return negocio;
    }

    private ObjectNode construirPoliticas() {
        ObjectNode politicas = mapper.createObjectNode();
        politicas.put("cancelacion", "Se puede cancelar hasta 24 horas antes sin coste. Cancelaciones tardías pueden suponer un cargo del 50%.");
        politicas.put("reservas", "Las citas se pueden reservar por teléfono o a través de la aplicación.");
        politicas.put("vacacionesEmpleados", "22 días laborables al año, solicitar con 15 días de antelación mínimo.");
        politicas.put("descuentoVip", "Los clientes VIP tienen descuento personalizado según su historial.");
        politicas.put("formasPago", "Efectivo, tarjeta de crédito/débito y transferencia bancaria.");
        return politicas;
    }

    private ArrayNode construirEquipo() {
        ArrayNode equipo = mapper.createArrayNode();
        for (Peluquero peluquero : peluqueroRepository.findAll()) {
            ObjectNode nodoPeluquero = mapper.createObjectNode();
            nodoPeluquero.put("nombre", peluquero.getNombre());
            if (peluquero.getEspecialidad() != null) {
                nodoPeluquero.put("especialidad", peluquero.getEspecialidad());
            }
            if (peluquero.getHorarioBase() != null) {
                nodoPeluquero.put("horario", peluquero.getHorarioBase());
            }
            nodoPeluquero.put("disponible",
                    peluquero.isDisponible() && !peluquero.isEnBaja() && !peluquero.isEnVacaciones());
            equipo.add(nodoPeluquero);
        }
        return equipo;
    }

    private ArrayNode construirCatalogoServicios() {
        ArrayNode catalogo = mapper.createArrayNode();
        for (Servicio servicio : servicioRepository.findAll()) {
            ObjectNode nodoServicio = mapper.createObjectNode();
            nodoServicio.put("nombre", servicio.getNombre());
            nodoServicio.put("precio",
                    servicio.getPrecio() != null ? servicio.getPrecio().doubleValue() : 0);
            nodoServicio.put("duracionMinutos",
                    servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : 0);
            if (servicio.getGenero() != null) {
                nodoServicio.put("genero", servicio.getGenero().name());
            }
            if (servicio.getCategoria() != null) {
                nodoServicio.put("categoria", servicio.getCategoria().name());
            }
            if (servicio.getDescripcion() != null) {
                nodoServicio.put("descripcion", servicio.getDescripcion());
            }
            catalogo.add(nodoServicio);
        }
        return catalogo;
    }

    private ArrayNode construirCatalogoProductos() {
        ArrayNode catalogo = mapper.createArrayNode();
        for (Producto producto : productoRepository.findAll()) {
            if (!producto.isActivo()) continue;
            ObjectNode nodoProducto = mapper.createObjectNode();
            nodoProducto.put("nombre", producto.getNombre());
            nodoProducto.put("precio",
                    producto.getPrecio() != null ? producto.getPrecio().doubleValue() : 0);
            if (producto.getCategoria() != null) {
                nodoProducto.put("categoria", producto.getCategoria().name());
            }
            if (producto.getGenero() != null) {
                nodoProducto.put("genero", producto.getGenero().name());
            }
            catalogo.add(nodoProducto);
        }
        return catalogo;
    }

    private ArrayNode construirOfertasActivas() {
        ArrayNode ofertasActivas = mapper.createArrayNode();
        List<Oferta> ofertas = ofertaRepository.findActivas();
        for (Oferta oferta : ofertas) {
            ObjectNode nodoOferta = mapper.createObjectNode();
            nodoOferta.put("nombre", oferta.getNombre());
            if (oferta.getDescripcion() != null) {
                nodoOferta.put("descripcion", oferta.getDescripcion());
            }
            if (oferta.getDescuentoPorcentaje() != null) {
                nodoOferta.put("descuentoPorcentaje", oferta.getDescuentoPorcentaje());
            }
            if (oferta.getFechaInicio() != null) {
                nodoOferta.put("fechaInicio", oferta.getFechaInicio().toString());
            }
            if (oferta.getFechaFin() != null) {
                nodoOferta.put("fechaFin", oferta.getFechaFin().toString());
            }
            if (oferta.getTipo() != null) {
                nodoOferta.put("tipo", oferta.getTipo().name());
            }
            ofertasActivas.add(nodoOferta);
        }
        return ofertasActivas;
    }
}
