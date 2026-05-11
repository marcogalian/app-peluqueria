package com.marcog.peluqueria.shared.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Notificaciones {

    private final JavaMailSender mailSender;

    @Value("${app.admin.email:admin@peluqueria.com}")
    private String adminEmail;

    public Notificaciones(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmail(String destinatario, String asunto, String cuerpo) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Email no enviado: destinatario vacío. Asunto: {}", asunto);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(destinatario);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            mailSender.send(msg);
            log.info("Email enviado a {} — {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
        }
    }

    public void notificarAdmin(String asunto, String cuerpo) {
        enviarEmail(adminEmail, asunto, cuerpo);
    }
}
