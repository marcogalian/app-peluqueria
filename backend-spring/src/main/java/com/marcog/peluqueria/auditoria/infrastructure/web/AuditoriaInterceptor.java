package com.marcog.peluqueria.auditoria.infrastructure.web;

import com.marcog.peluqueria.auditoria.application.RegistrarActividad;
import com.marcog.peluqueria.auditoria.domain.RegistroActividad;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuditoriaInterceptor implements HandlerInterceptor {
    private static final Set<String> METODOS_AUDITADOS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RegistrarActividad registrarActividad;

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
                .metodoHttp(metodo)
                .ruta(ruta)
                .estadoHttp(response.getStatus())
                .build());
    }

    private boolean debeIgnorarse(String ruta) {
        return ruta.startsWith("/api/v1/auth")
                || ruta.startsWith("/api/auth")
                || ruta.startsWith("/api/v1/auditoria")
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

    private String capitalizar(String valor) {
        if (valor.isBlank()) {
            return "Sistema";
        }
        return valor.substring(0, 1).toUpperCase(Locale.ROOT) + valor.substring(1);
    }
}
