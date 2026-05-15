package com.marcog.peluqueria.shared.infrastructure.web;

import com.marcog.peluqueria.citas.domain.exception.CitaSuperpuestaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todos los controladores REST.
 * Convierte las excepciones del dominio y de la capa de aplicación en respuestas HTTP con código de estado adecuado.
 * Centraliza el manejo de errores para que ningún controlador necesite capturar excepciones manualmente.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : mensajePorDefecto(status);
        return build(status, message, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatearCampo)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Los datos enviados no son válidos.";
        }

        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    /** Entidad no encontrada → 404 */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiErrorResponse> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, mensajeSeguro(ex, "Recurso no encontrado."), request.getRequestURI());
    }

    /** Cita superpuesta con otra ya existente → 409 */
    @ExceptionHandler(CitaSuperpuestaException.class)
    public ResponseEntity<ApiErrorResponse> handleCitaSuperpuesta(
            CitaSuperpuestaException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, mensajeSeguro(ex, "El horario solicitado ya está ocupado."), request.getRequestURI());
    }

    @ExceptionHandler({ IllegalArgumentException.class, ConstraintViolationException.class })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, mensajeSeguro(ex, "Los datos enviados no son válidos."), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", request.getRequestURI());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", request.getRequestURI());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        if (pareceNoEncontrado(ex)) {
            return build(HttpStatus.NOT_FOUND, mensajeSeguro(ex, "Recurso no encontrado."), request.getRequestURI());
        }

        log.error("Error interno no controlado en {} {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getMessage(),
                ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno. Inténtalo de nuevo o vuelve al panel.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(),
                status.name(),
                message,
                path,
                Instant.now()
        ));
    }

    private String formatearCampo(FieldError error) {
        String defaultMessage = error.getDefaultMessage() != null ? error.getDefaultMessage() : "valor no válido";
        return error.getField() + ": " + defaultMessage;
    }

    private boolean pareceNoEncontrado(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null) return false;

        String normalizado = message.toLowerCase(Locale.ROOT);
        return normalizado.contains("not found")
                || normalizado.contains("no encontrado")
                || normalizado.contains("no encontrada");
    }

    private String mensajeSeguro(RuntimeException ex, String fallback) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private String mensajePorDefecto(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "La petición no es válida.";
            case UNAUTHORIZED -> "Debes iniciar sesión para continuar.";
            case FORBIDDEN -> "No tienes permisos para realizar esta acción.";
            case NOT_FOUND -> "Recurso no encontrado.";
            case CONFLICT -> "La operación entra en conflicto con el estado actual.";
            default -> "No se pudo completar la operación.";
        };
    }

    public record ApiErrorResponse(
            int status,
            String code,
            String message,
            String path,
            Instant timestamp
    ) {}
}
