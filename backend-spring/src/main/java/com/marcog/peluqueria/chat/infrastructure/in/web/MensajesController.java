package com.marcog.peluqueria.chat.infrastructure.in.web;

import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.PeluqueroEntity;
import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import com.marcog.peluqueria.shared.notification.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Mensajes", description = "Contactos y comunicacion por email")
@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
@Slf4j
public class MensajesController {

    private final JpaUserRepository userRepository;
    private final JpaPeluqueroRepository peluqueroRepository;
    private final NotificationService notificationService;

    /**
     * Devuelve la lista de contactos disponibles para chatear o escribir email.
     * Admin → todos los peluqueros.
     * Peluquero → el admin + el resto de peluqueros.
     */
    @Operation(summary = "Listar contactos", description = "Retorna los contactos disponibles segun el rol del usuario")
    @ApiResponse(responseCode = "200", description = "Lista de contactos")
    @GetMapping("/contactos")
    public ResponseEntity<List<ContactoDTO>> contactos(Authentication auth) {
        return ResponseEntity.ok(construirContactos(auth));
    }

    /** Historial vacío — los mensajes solo viajan por WebSocket sin persistencia */
    @Operation(summary = "Historial mensajes", description = "Retorna el historial de mensajes con un contacto")
    @ApiResponse(responseCode = "200", description = "Historial (vacio, mensajes via WebSocket)")
    @GetMapping("/historial/{contactoId}")
    public ResponseEntity<List<Object>> historial(@PathVariable UUID contactoId) {
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Enviar email", description = "Envia un email manual a un contacto")
    @ApiResponse(responseCode = "200", description = "Email enviado")
    @ApiResponse(responseCode = "400", description = "Contacto sin email")
    @PostMapping("/email/{contactoId}")
    public ResponseEntity<EmailEnviadoDTO> enviarEmailManual(
            @PathVariable UUID contactoId,
            @Valid @RequestBody EmailManualRequest request,
            Authentication auth
    ) {
        ContactoDTO contacto = obtenerContactoVisible(contactoId, auth);

        if (contacto.getEmail() == null || contacto.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contacto no tiene email configurado.");
        }

        String asunto = request.getAsunto().trim();
        String cuerpo = request.getCuerpo().trim();

        notificationService.enviarEmail(contacto.getEmail(), asunto, cuerpo);
        log.info("Email manual enviado por {} a {} <{}>", auth.getName(), contacto.getNombre(), contacto.getEmail());

        return ResponseEntity.ok(EmailEnviadoDTO.builder()
                .destinatario(contacto.getEmail())
                .asunto(asunto)
                .mensaje("Email enviado correctamente mediante Mailtrap.")
                .build());
    }

    private List<ContactoDTO> construirContactos(Authentication auth) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<ContactoDTO> resultado = new ArrayList<>();

        if (esAdmin) {
            peluqueroRepository.findAll().forEach(p -> resultado.add(desdePeluquero(p)));
        } else {
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ROLE_ADMIN)
                    .forEach(u -> resultado.add(desdeUser(u)));

            String username = auth.getName();
            peluqueroRepository.findAll().stream()
                    .filter(p -> !username.equals(p.getUser().getUsername()))
                    .forEach(p -> resultado.add(desdePeluquero(p)));
        }

        return resultado;
    }

    private ContactoDTO obtenerContactoVisible(UUID contactoId, Authentication auth) {
        return construirContactos(auth).stream()
                .filter(c -> contactoId.toString().equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contacto no disponible."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ContactoDTO desdePeluquero(PeluqueroEntity p) {
        UserEntity u = p.getUser();
        return ContactoDTO.builder()
                .id(p.getId().toString())
                .nombre(p.getNombre())
                .iniciales(iniciales(p.getNombre()))
                .rol(u.getRole().name())
                .email(u.getEmail())
                .online(false)
                .build();
    }

    private ContactoDTO desdeUser(UserEntity u) {
        return ContactoDTO.builder()
                .id(u.getId().toString())
                .nombre(u.getUsername())
                .iniciales(iniciales(u.getUsername()))
                .rol(u.getRole().name())
                .email(u.getEmail())
                .online(false)
                .build();
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "??";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        return (partes[0].charAt(0) + "" + partes[1].charAt(0)).toUpperCase();
    }

    @Data
    public static class EmailManualRequest {
        @NotBlank
        @Size(max = 160)
        private String asunto;

        @NotBlank
        @Size(max = 4000)
        private String cuerpo;
    }

    @Data
    @Builder
    public static class EmailEnviadoDTO {
        private String destinatario;
        private String asunto;
        private String mensaje;
    }

    @Data
    @Builder
    public static class ContactoDTO {
        private String id;
        private String nombre;
        private String iniciales;
        private String rol;
        private String email;
        private String telefono;
        private boolean online;
    }
}
