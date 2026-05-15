package com.marcog.peluqueria.chatbot.infrastructure.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marcog.peluqueria.chatbot.domain.ContextoNegocio;
import com.marcog.peluqueria.configuracion.application.GestionarConfiguracion;
import com.marcog.peluqueria.configuracion.infrastructure.persistence.ConfiguracionEntity;
import com.marcog.peluqueria.ofertas.domain.Oferta;
import com.marcog.peluqueria.ofertas.domain.OfertaRepository;
import com.marcog.peluqueria.peluqueros.domain.Peluquero;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.productos.domain.Producto;
import com.marcog.peluqueria.productos.domain.ProductoRepository;
import com.marcog.peluqueria.servicios.domain.Servicio;
import com.marcog.peluqueria.servicios.domain.ServicioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Cachea en memoria el contexto estatico del negocio en formato JSON.
 *
 * Lo que entra aqui debe cambiar como mucho una vez al dia (servicios, productos,
 * politicas, equipo, ofertas). Los datos en tiempo real (citas, dinero, stock)
 * NO se cachean: van por function calling en ConsultasGestionPeluqueriaPostgres.
 *
 * Se regenera al arrancar la aplicacion y cada noche a las 03:00.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArchivoContextoNegocio implements ContextoNegocio {

    // ── Dependencias ────────────────────────────────────────────────
    private final ServicioRepository servicioRepository;
    private final ProductoRepository productoRepository;
    private final PeluqueroRepository peluqueroRepository;
    private final OfertaRepository ofertaRepository;
    private final GestionarConfiguracion GestionarConfiguracion;
    // ObjectMapper inyectado de Spring (autoconfigurado, con modulo JavaTime)
    // en lugar de new ObjectMapper() para mantener configuracion consistente.
    private final ObjectMapper mapper;

    @Value("${chatbot.context-file:./data/chatbot/contexto-negocio.json}")
    private String contextFile;

    private volatile String cachedContext = "";

    // ── Ciclo de vida ───────────────────────────────────────────────

    @Override
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

            cachedContext = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(raiz);
            guardarContextoVisible(cachedContext);
            log.info("Contexto del chatbot regenerado correctamente");
        } catch (Exception ex) {
            log.error("Error regenerando contexto del chatbot: {}", ex.getMessage());
        }
    }

    @Override
    public String getContext() {
        return cachedContext;
    }

    private void guardarContextoVisible(String json) {
        try {
            Path destino = Path.of(contextFile);
            Path carpeta = destino.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Files.writeString(destino, json, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("No se pudo escribir el contexto visible del chatbot: {}", ex.getMessage());
        }
    }

    // ── Construccion del JSON ───────────────────────────────────────

    private ObjectNode construirNegocio() {
        // Lee la configuracion del centro desde BD para que los datos del chatbot
        // (horario, telefono, email, direccion) reflejen lo que el admin ha guardado.
        ConfiguracionEntity config = GestionarConfiguracion.obtener();

        ObjectNode negocio = mapper.createObjectNode();
        negocio.put("nombre", config.getNombreNegocio());
        negocio.put("direccion", config.getDireccion());
        negocio.put("telefono", config.getTelefono());
        negocio.put("email", config.getEmail());

        ObjectNode horario = mapper.createObjectNode();
        String apertura = config.getHorarioApertura() != null ? config.getHorarioApertura() : "09:00";
        String cierre = config.getHorarioCierre() != null ? config.getHorarioCierre() : "21:00";
        String pausaInicio = config.getHorarioPausaInicio();
        String pausaFin = config.getHorarioPausaFin();
        String aperturaSabado = config.getHorarioAperturaSabado() != null ? config.getHorarioAperturaSabado() : apertura;
        String cierreSabado = config.getHorarioCierreSabado() != null ? config.getHorarioCierreSabado() : "14:00";

        horario.put("lunesViernes", formatearHorario(apertura, pausaInicio, pausaFin, cierre));
        horario.put("sabado", config.isAbreSabado() ? (aperturaSabado + " - " + cierreSabado) : "Cerrado");
        horario.put("domingo", config.isAbreDomingo() ? (apertura + " - " + cierreSabado) : "Cerrado");
        negocio.set("horario", horario);

        return negocio;
    }

    private String formatearHorario(String apertura, String pausaInicio, String pausaFin, String cierre) {
        if (pausaInicio == null || pausaInicio.isBlank() || pausaFin == null || pausaFin.isBlank()) {
            return apertura + " - " + cierre;
        }
        return apertura + " - " + pausaInicio + " y " + pausaFin + " - " + cierre;
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
            if (servicio.getPrecioDescuento() != null) {
                nodoServicio.put("precioDescuento", servicio.getPrecioDescuento().doubleValue());
            }
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
            nodoProducto.put("stock", producto.getStock() != null ? producto.getStock() : 0);
            nodoProducto.put("stockMinimo", producto.getStockMinimo() != null ? producto.getStockMinimo() : 0);
            nodoProducto.put("stockBajo",
                    producto.getStock() != null
                            && producto.getStockMinimo() != null
                            && producto.getStock() <= producto.getStockMinimo());
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
