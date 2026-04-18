package com.marcog.peluqueria.shared.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${twilio.account.sid:}")
    private String twilioSid;

    @Value("${twilio.auth.token:}")
    private String twilioToken;

    @Value("${twilio.whatsapp.from:}")
    private String twilioFrom;

    @Value("${app.admin.email:admin@peluqueria.com}")
    private String adminEmail;

    @Value("${app.admin.phone:}")
    private String adminPhone;

    private boolean twilioEnabled = false;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    public void init() {
        if (twilioSid != null && !twilioSid.isBlank() && twilioToken != null && !twilioToken.isBlank()) {
            Twilio.init(twilioSid, twilioToken);
            twilioEnabled = true;
            log.info("NotificationService: Twilio inicializado.");
        } else {
            log.warn("NotificationService: Twilio no configurado — WhatsApp en modo SIMULACIÓN.");
        }
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

    public void enviarWhatsApp(String telefono, String mensaje) {
        if (telefono == null || telefono.isBlank()) {
            log.warn("WhatsApp no enviado: teléfono vacío.");
            return;
        }
        if (twilioEnabled) {
            try {
                String to = telefono.startsWith("whatsapp:") ? telefono : "whatsapp:" + telefono;
                String from = twilioFrom.startsWith("whatsapp:") ? twilioFrom : "whatsapp:" + twilioFrom;
                Message msg = Message.creator(new PhoneNumber(to), new PhoneNumber(from), mensaje).create();
                log.info("WhatsApp enviado a {} — SID: {}", telefono, msg.getSid());
            } catch (Exception e) {
                log.error("Error enviando WhatsApp a {}: {}", telefono, e.getMessage());
            }
        } else {
            log.info("==== SIMULACIÓN WHATSAPP ====");
            log.info("Para: {}", telefono);
            log.info("Mensaje: {}", mensaje);
            log.info("=============================");
        }
    }

    public void notificarAdmin(String asunto, String cuerpo) {
        enviarEmail(adminEmail, asunto, cuerpo);
        if (!adminPhone.isBlank()) {
            enviarWhatsApp(adminPhone, asunto + "\n" + cuerpo);
        }
    }
}
