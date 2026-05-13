package com.marcog.peluqueria.chat.infrastructure.web;

import com.marcog.peluqueria.chat.infrastructure.persistence.JpaMensajeInternoRepository;
import com.marcog.peluqueria.chat.infrastructure.persistence.MensajeInternoEntity;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.PeluqueroEntity;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.shared.notification.Notificaciones;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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
    private final JpaMensajeInternoRepository mensajeInternoRepository;
    private final Notificaciones notificaciones;

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

    @Operation(summary = "Contar mensajes no leidos", description = "Retorna cuantos mensajes internos tiene pendientes el usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Conteo de mensajes no leidos")
    @GetMapping("/no-leidos")
    public ResponseEntity<NoLeidosDTO> contarNoLeidos(Authentication auth) {
        UserEntity usuario = usuarioActual(auth);
        long total = mensajeInternoRepository.countByReceptorUserIdAndLeidoFalseAndArchivadoFalse(usuario.getId());
        return ResponseEntity.ok(NoLeidosDTO.builder().total(total).build());
    }

    @Operation(summary = "Marcar mensajes como leidos", description = "Marca como leidos todos los mensajes internos recibidos por el usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Mensajes marcados como leidos")
    @PatchMapping("/no-leidos/marcar-leidos")
    @Transactional
    public ResponseEntity<NoLeidosDTO> marcarNoLeidosComoLeidos(Authentication auth) {
        UserEntity usuario = usuarioActual(auth);
        mensajeInternoRepository.marcarRecibidosComoLeidos(usuario.getId());
        return ResponseEntity.ok(NoLeidosDTO.builder().total(0).build());
    }

    @Operation(summary = "Historial mensajes", description = "Retorna el historial de mensajes con un contacto")
    @ApiResponse(responseCode = "200", description = "Historial de mensajes")
    @GetMapping("/historial/{contactoId}")
    public ResponseEntity<List<MensajeDTO>> historial(@PathVariable UUID contactoId, Authentication auth) {
        UserEntity usuario = usuarioActual(auth);
        ContactoDTO contacto = obtenerContactoVisible(contactoId, auth);

        List<MensajeDTO> historial = mensajeInternoRepository
                .findConversacion(usuario.getId(), contacto.getUserId())
                .stream()
                .map(this::aDto)
                .toList();

        return ResponseEntity.ok(historial);
    }

    @Operation(summary = "Archivar mensaje", description = "Marca un mensaje como archivado para ocultarlo de la bandeja activa")
    @ApiResponse(responseCode = "200", description = "Mensaje archivado")
    @PatchMapping("/{mensajeId}/archivar")
    public ResponseEntity<MensajeDTO> archivarMensaje(@PathVariable UUID mensajeId, Authentication auth) {
        MensajeInternoEntity mensaje = obtenerMensajeVisibleParaUsuario(mensajeId, auth);
        mensaje.setArchivado(true);
        return ResponseEntity.ok(aDto(mensajeInternoRepository.save(mensaje)));
    }

    @Operation(summary = "Eliminar mensaje", description = "Elimina un mensaje interno")
    @ApiResponse(responseCode = "204", description = "Mensaje eliminado")
    @DeleteMapping("/{mensajeId}")
    public ResponseEntity<Void> eliminarMensaje(@PathVariable UUID mensajeId, Authentication auth) {
        MensajeInternoEntity mensaje = obtenerMensajeVisibleParaUsuario(mensajeId, auth);
        mensajeInternoRepository.delete(mensaje);
        return ResponseEntity.noContent().build();
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

        notificaciones.enviarEmail(contacto.getEmail(), asunto, cuerpo);
        MensajeInternoEntity mensaje = mensajeInternoRepository.save(MensajeInternoEntity.builder()
                .emisorUserId(usuarioActual(auth).getId())
                .receptorUserId(contacto.getUserId())
                .asunto(asunto)
                .contenido(cuerpo)
                .enviadoEn(LocalDateTime.now())
                .build());

        log.info("Email manual enviado por {} a {} <{}>", auth.getName(), contacto.getNombre(), contacto.getEmail());

        return ResponseEntity.ok(EmailEnviadoDTO.builder()
                .destinatario(contacto.getEmail())
                .asunto(asunto)
                .mensaje("Email enviado correctamente mediante Mailtrap.")
                .mensajeChat(aDto(mensaje))
                .build());
    }

    private UserEntity usuarioActual(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado."));
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

    private MensajeInternoEntity obtenerMensajeVisibleParaUsuario(UUID mensajeId, Authentication auth) {
        UserEntity usuario = usuarioActual(auth);
        return mensajeInternoRepository.findById(mensajeId)
                .filter(m -> usuario.getId().equals(m.getEmisorUserId()) || usuario.getId().equals(m.getReceptorUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no disponible."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ContactoDTO desdePeluquero(PeluqueroEntity p) {
        UserEntity u = p.getUser();
        return ContactoDTO.builder()
                .id(p.getId().toString())
                .userId(u.getId())
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
                .userId(u.getId())
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

    private MensajeDTO aDto(MensajeInternoEntity mensaje) {
        return MensajeDTO.builder()
                .id(mensaje.getId().toString())
                .emisorId(usernameUsuario(mensaje.getEmisorUserId()))
                .emisorNombre(nombreUsuario(mensaje.getEmisorUserId()))
                .asunto(mensaje.getAsunto())
                .contenido(mensaje.getContenido())
                .enviadoEn(mensaje.getEnviadoEn().toString())
                .leido(mensaje.isLeido())
                .archivado(mensaje.isArchivado())
                .build();
    }

    private String nombreUsuario(UUID userId) {
        return peluqueroRepository.findAll().stream()
                .filter(p -> p.getUser() != null && userId.equals(p.getUser().getId()))
                .map(PeluqueroEntity::getNombre)
                .findFirst()
                .orElseGet(() -> userRepository.findById(userId)
                        .map(UserEntity::getUsername)
                        .orElse("Usuario"));
    }

    private String usernameUsuario(UUID userId) {
        return userRepository.findById(userId)
                .map(UserEntity::getUsername)
                .orElse(userId.toString());
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
        private MensajeDTO mensajeChat;
    }

    @Data
    @Builder
    public static class ContactoDTO {
        private String id;
        private UUID userId;
        private String nombre;
        private String iniciales;
        private String rol;
        private String email;
        private String telefono;
        private boolean online;
    }

    @Data
    @Builder
    public static class MensajeDTO {
        private String id;
        private String emisorId;
        private String emisorNombre;
        private String asunto;
        private String contenido;
        private String enviadoEn;
        private boolean leido;
        private boolean archivado;
    }

    @Data
    @Builder
    public static class NoLeidosDTO {
        private long total;
    }
}
