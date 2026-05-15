package com.marcog.peluqueria.auditoria.infrastructure.web;

import com.marcog.peluqueria.auditoria.application.RegistrarActividad;
import com.marcog.peluqueria.auditoria.domain.RegistroActividad;
import com.marcog.peluqueria.clientes.domain.ClienteRepository;
import com.marcog.peluqueria.ofertas.domain.OfertaRepository;
import com.marcog.peluqueria.peluqueros.domain.PeluqueroRepository;
import com.marcog.peluqueria.productos.domain.ProductoRepository;
import com.marcog.peluqueria.servicios.domain.ServicioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Interceptor de Spring MVC que registra en auditoría cada operación de escritura (POST, PUT, PATCH, DELETE).
 * Se ejecuta después de que el controlador ha procesado la petición (afterCompletion).
 * Resuelve nombres legibles de las entidades afectadas consultando los repositorios del dominio.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {
    private static final Set<String> METODOS_AUDITADOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RegistrarActividad registrarActividad;
    private final PeluqueroRepository peluqueroRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final OfertaRepository ofertaRepository;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();

        if (!METODOS_AUDITADOS.contains(metodo) || debeIgnorarse(ruta)) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return;
        }

        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("SIN_ROL");

        registrarActividad.registrar(RegistroActividad.builder()
                .usuario(auth.getName())
                .rol(rol)
                .accion(describirAccion(metodo))
                .modulo(resolverModulo(ruta))
                .detalle(describirDetalleNegocio(metodo, ruta, request))
                .entidadId(resolverEntidadId(request))
                .metodoHttp(metodo)
                .ruta(ruta)
                .estadoHttp(response.getStatus())
                .build());
    }

    private boolean debeIgnorarse(String ruta) {
        return ruta.startsWith("/api/v1/auth")
                || ruta.startsWith("/api/auth")
                || ruta.startsWith("/api/v1/auditoria")
                || ruta.startsWith("/api/chat")
                || ruta.startsWith("/api/v1/chat")
                || ruta.startsWith("/api-docs")
                || ruta.startsWith("/swagger-ui");
    }

    private String describirAccion(String metodo) {
        return switch (metodo) {
            case "POST" -> "CREACION";
            case "PUT", "PATCH" -> "MODIFICACION";
            case "DELETE" -> "ELIMINACION";
            default -> "ACCION";
        };
    }

    private String resolverModulo(String ruta) {
        String normalizada = ruta.toLowerCase(Locale.ROOT);
        String[] partes = normalizada.split("/");

        for (String parte : partes) {
            if (parte.isBlank() || parte.equals("api") || parte.equals("v1")) {
                continue;
            }
            return switch (parte) {
                case "citas" -> "Citas";
                case "clientes" -> "Clientes";
                case "peluqueros" -> "Peluqueros";
                case "productos" -> "Inventario";
                case "servicios" -> "Servicios";
                case "ausencias" -> "Vacaciones";
                case "mensajes" -> "Mensajes";
                case "ofertas" -> "Ofertas";
                case "configuracion" -> "Configuracion";
                case "finanzas" -> "Finanzas";
                default -> capitalizar(parte.replace("-", " "));
            };
        }

        return "Sistema";
    }

    private String describirDetalleNegocio(String metodo, String ruta, HttpServletRequest request) {
        String normalizada = ruta.toLowerCase(Locale.ROOT);
        String entidadId = resolverEntidadId(request);

        if (normalizada.contains("/clientes")) {
            String nombre = resolverNombreCliente(entidadId);
            if (normalizada.endsWith("/archivar")) return "Cliente archivado: " + nombre;
            if (normalizada.endsWith("/reactivar")) return "Cliente reactivado: " + nombre;
            if (normalizada.endsWith("/consentimiento")) return "Consentimiento de fotos actualizado: " + nombre;
            return switch (metodo) {
                case "POST" -> "Cliente creado";
                case "PUT", "PATCH" -> "Cliente modificado: " + nombre;
                case "DELETE" -> "Cliente eliminado: " + nombre;
                default -> "Cliente actualizado: " + nombre;
            };
        }

        if (normalizada.contains("/productos")) {
            String nombre = resolverNombreProducto(entidadId);
            if (normalizada.contains("/ventas")) return "Venta de productos registrada";
            if (normalizada.endsWith("/vender")) return "Producto vendido: " + nombre;
            if (normalizada.endsWith("/stock")) return "Stock ajustado: " + nombre;
            return switch (metodo) {
                case "POST" -> "Producto creado";
                case "PUT", "PATCH" -> "Producto modificado: " + nombre;
                case "DELETE" -> "Producto desactivado: " + nombre;
                default -> "Producto actualizado: " + nombre;
            };
        }

        if (normalizada.contains("/servicios")) {
            String nombre = resolverNombreServicio(entidadId);
            return switch (metodo) {
                case "POST" -> "Servicio creado";
                case "PUT", "PATCH" -> "Servicio modificado: " + nombre;
                case "DELETE" -> "Servicio eliminado: " + nombre;
                default -> "Servicio actualizado: " + nombre;
            };
        }

        if (normalizada.contains("/citas")) {
            String sufijoId = entidadId == null ? "" : " (" + entidadId + ")";
            if (normalizada.endsWith("/cancelar")) return "Cita cancelada" + sufijoId;
            return switch (metodo) {
                case "POST" -> "Cita creada";
                case "PUT", "PATCH" -> "Cita modificada" + sufijoId;
                case "DELETE" -> "Cita eliminada" + sufijoId;
                default -> "Cita actualizada" + sufijoId;
            };
        }

        if (normalizada.contains("/peluqueros")) {
            String nombre = resolverNombrePeluquero(entidadId);
            return switch (metodo) {
                case "POST" -> "Empleado creado";
                case "PUT", "PATCH" -> "Empleado modificado: " + nombre;
                case "DELETE" -> "Empleado eliminado: " + nombre;
                default -> "Empleado actualizado: " + nombre;
            };
        }

        if (normalizada.contains("/ausencias")) {
            String sufijoId = entidadId == null ? "" : " (" + entidadId + ")";
            return switch (metodo) {
                case "POST" -> "Ausencia o vacaciones solicitadas";
                case "PUT", "PATCH" -> "Ausencia o vacaciones modificadas" + sufijoId;
                case "DELETE" -> "Ausencia o vacaciones eliminadas" + sufijoId;
                default -> "Ausencia o vacaciones actualizadas" + sufijoId;
            };
        }

        if (normalizada.contains("/ofertas")) {
            String nombre = resolverNombreOferta(entidadId);
            return switch (metodo) {
                case "POST" -> "Oferta creada";
                case "PUT", "PATCH" -> "Oferta modificada: " + nombre;
                case "DELETE" -> "Oferta eliminada: " + nombre;
                default -> "Oferta actualizada: " + nombre;
            };
        }

        if (normalizada.contains("/configuracion")) {
            return "Configuracion del salon modificada";
        }

        if (normalizada.contains("/finanzas")) {
            String sufijoId = entidadId == null ? "" : " (" + entidadId + ")";
            return switch (metodo) {
                case "POST" -> "Gasto registrado";
                case "PUT", "PATCH" -> "Gasto modificado" + sufijoId;
                case "DELETE" -> "Gasto eliminado" + sufijoId;
                default -> "Finanzas actualizadas" + sufijoId;
            };
        }

        String sufijoId = entidadId == null ? "" : " (" + entidadId + ")";
        return resolverModulo(ruta) + " actualizado" + sufijoId;
    }

    private String resolverNombrePeluquero(String id) {
        if (id == null) return "";
        try {
            return peluqueroRepository.findById(UUID.fromString(id))
                    .map(p -> p.getNombre())
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    private String resolverNombreCliente(String id) {
        if (id == null) return "";
        try {
            return clienteRepository.findById(UUID.fromString(id))
                    .map(c -> c.getNombre() + (c.getApellidos() != null ? " " + c.getApellidos() : ""))
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    private String resolverNombreProducto(String id) {
        if (id == null) return "";
        try {
            return productoRepository.findById(UUID.fromString(id))
                    .map(p -> p.getNombre())
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    private String resolverNombreServicio(String id) {
        if (id == null) return "";
        try {
            return servicioRepository.findById(UUID.fromString(id))
                    .map(s -> s.getNombre())
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    private String resolverNombreOferta(String id) {
        if (id == null) return "";
        try {
            return ofertaRepository.findById(UUID.fromString(id))
                    .map(o -> o.getNombre())
                    .orElse(id);
        } catch (Exception e) {
            return id;
        }
    }

    @SuppressWarnings("unchecked")
    private String resolverEntidadId(HttpServletRequest request) {
        Object atributos = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(atributos instanceof Map<?, ?> variables)) {
            return null;
        }

        Object id = variables.get("id");
        return id == null ? null : id.toString();
    }

    private String capitalizar(String valor) {
        if (valor.isBlank()) {
            return "Sistema";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1);
    }
}
