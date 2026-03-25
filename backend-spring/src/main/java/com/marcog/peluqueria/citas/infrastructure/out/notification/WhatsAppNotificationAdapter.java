package com.marcog.peluqueria.citas.infrastructure.out.notification;

import com.marcog.peluqueria.citas.domain.model.Cita;
import com.marcog.peluqueria.citas.domain.port.out.NotificationPort;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Slf4j
public class WhatsAppNotificationAdapter implements NotificationPort {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.from:}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio API inicializada correctamente.");
        } else {
            log.warn(
                    "Credenciales de Twilio no encontradas. Las notificaciones WhatsApp funcionarán en modo SIMULACIÓN.");
        }
    }

    @Override
    public void enviarRecordatorio(Cita cita) {
        String telefono = cita.getCliente() != null ? cita.getCliente().getTelefono() : "Sin teléfono";
        String nombre = cita.getCliente() != null ? cita.getCliente().getNombre() : "Cliente";
        String peluquero = cita.getPeluquero() != null ? cita.getPeluquero().getNombre() : "asignado";
        String fecha = cita.getFechaHora().toString();

        String body = String.format(
                "Hola %s, te recordamos que tienes una cita mañana a las %s con el peluquero %s. ¡Te esperamos!",
                nombre, fecha, peluquero);

        if (accountSid != null && !accountSid.isEmpty() && authToken != null && !authToken.isEmpty()) {
            try {
                // Ensure the phone numbers start with "whatsapp:"
                String to = telefono.startsWith("whatsapp:") ? telefono : "whatsapp:" + telefono;
                String from = fromNumber.startsWith("whatsapp:") ? fromNumber : "whatsapp:" + fromNumber;

                Message message = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(from),
                        body).create();

                log.info("WhatsApp enviado. SID: {}", message.getSid());
            } catch (Exception e) {
                log.error("Error enviando WhatsApp vía Twilio: {}", e.getMessage());
            }
        } else {
            log.info("========== SIMULACIÓN WHATSAPP ==========");
            log.info("Para: {}", telefono);
            log.info("Mensaje: {}", body);
            log.info("=========================================");
        }
    }
}
