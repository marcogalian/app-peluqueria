package com.marcog.peluqueria.citas.infrastructure.out.notification;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationAdapter implements NotificationPort {

    private final JavaMailSender javaMailSender;

    @Override
    public void enviarRecordatorio(Cita cita) {
        String email = cita.getCliente() != null ? cita.getCliente().getEmail() : null;
        if (email == null || email.isBlank()) {
            log.warn("No se puede enviar email para la cita ID: {} porque el cliente no tiene email registrado.",
                    cita.getId());
            return;
        }

        String nombre = cita.getCliente() != null ? cita.getCliente().getNombre() : "Cliente";
        String peluquero = cita.getPeluquero() != null ? cita.getPeluquero().getNombre() : "asignado";

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject("Recordatorio de Cita en Peluquería Marco G");
            mailMessage.setText(String.format(
                    "Hola %s,\n\nTe recordamos que tienes una cita programada mañana a las %s con el peluquero %s.\n\n¡Te esperamos!\n\nSaludos,\nEl equipo de Peluquería Marco G",
                    nombre, cita.getFechaHora(), peluquero));

            javaMailSender.send(mailMessage);
            log.info("Email de recordatorio enviado correctamente a: {}", email);
        } catch (Exception e) {
            log.error("Error al enviar el email de recordatorio a {}: {}", email, e.getMessage());
        }
    }
}
