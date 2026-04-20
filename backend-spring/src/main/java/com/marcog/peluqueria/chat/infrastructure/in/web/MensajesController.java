package com.marcog.peluqueria.chat.infrastructure.in.web;

import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.peluqueros.infrastructure.out.persistence.PeluqueroEntity;
import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensajes")
@RequiredArgsConstructor
public class MensajesController {

    private final JpaUserRepository      userRepository;
    private final JpaPeluqueroRepository peluqueroRepository;

    /**
     * Devuelve la lista de contactos disponibles para chatear.
     * Admin → todos los peluqueros.
     * Peluquero → el admin + el resto de peluqueros.
     */
    @GetMapping("/contactos")
    public ResponseEntity<List<ContactoDTO>> contactos(Authentication auth) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<ContactoDTO> resultado = new ArrayList<>();

        if (esAdmin) {
            // Admin ve a todos los peluqueros
            peluqueroRepository.findAll().forEach(p ->
                    resultado.add(desdePeluquero(p)));
        } else {
            // Peluquero ve al admin
            userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ROLE_ADMIN)
                    .forEach(u -> resultado.add(desdeUser(u)));

            // Y al resto de peluqueros (excepto él mismo)
            String username = auth.getName();
            peluqueroRepository.findAll().stream()
                    .filter(p -> !username.equals(p.getUser().getUsername()))
                    .forEach(p -> resultado.add(desdePeluquero(p)));
        }

        return ResponseEntity.ok(resultado);
    }

    /** Historial vacío — los mensajes solo viajan por WebSocket sin persistencia */
    @GetMapping("/historial/{contactoId}")
    public ResponseEntity<List<Object>> historial(@PathVariable UUID contactoId) {
        return ResponseEntity.ok(List.of());
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

    @Data @Builder
    public static class ContactoDTO {
        private String  id;
        private String  nombre;
        private String  iniciales;
        private String  rol;
        private String  email;
        private String  telefono;
        private boolean online;
    }
}
