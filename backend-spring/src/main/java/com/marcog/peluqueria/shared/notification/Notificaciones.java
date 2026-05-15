package com.marcog.peluqueria.shared.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Notificaciones {

    private final JavaMailSender mailSender;

    @Value("${app.admin.email:admin@email.com}")
    private String adminEmail;

    @Value("${spring.mail.username:noreply@peluqueria.com}")
    private String fromEmail;

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
            msg.setFrom(fromEmail);
            msg.setTo(destinatario);
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            log.info("Enviando email a {} desde {} — {}", destinatario, fromEmail, asunto);
            mailSender.send(msg);
            log.info("Email enviado OK a {} — {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("Error enviando email a {} (from={}): {}", destinatario, fromEmail, e.getMessage(), e);
        }
    }

    public void enviarEmailConBoton(String destinatario, String asunto, String introduccion, String textoBoton, String enlace) {
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Email no enviado: destinatario vacío. Asunto: {}", asunto);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(construirHtml(introduccion, textoBoton, enlace), true);
            log.info("Enviando email HTML a {} desde {} — {}", destinatario, fromEmail, asunto);
            mailSender.send(mime);
            log.info("Email HTML enviado OK a {} — {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("Error enviando email HTML a {} (from={}): {}", destinatario, fromEmail, e.getMessage(), e);
        }
    }

    public void notificarAdmin(String asunto, String cuerpo) {
        enviarEmail(adminEmail, asunto, cuerpo);
    }

    private String construirHtml(String introduccion, String textoBoton, String enlace) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#f4f4f7;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr><td align="center" style="padding:40px 16px;">
                      <table width="520" cellpadding="0" cellspacing="0"
                             style="background:#fff;border-radius:12px;box-shadow:0 2px 8px rgba(0,0,0,.08);overflow:hidden;">
                        <tr>
                          <td style="background:#1a2d5a;padding:28px 32px;">
                            <h1 style="margin:0;color:#fff;font-size:20px;font-weight:700;">
                              ✂ Peluquería Isabella
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;">
                            <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.6;">
                              %s
                            </p>
                            <div style="text-align:center;margin:32px 0;">
                              <a href="%s"
                                 style="display:inline-block;background:#1a2d5a;color:#fff;
                                        text-decoration:none;padding:14px 32px;border-radius:8px;
                                        font-size:15px;font-weight:600;">
                                %s
                              </a>
                            </div>
                            <p style="margin:24px 0 0;color:#9ca3af;font-size:12px;text-align:center;">
                              Si no has solicitado este cambio, ignora este mensaje.<br/>
                              El enlace caduca en 30 minutos.
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(introduccion, enlace, textoBoton);
    }
}
