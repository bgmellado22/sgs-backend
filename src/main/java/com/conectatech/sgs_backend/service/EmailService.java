package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.event.IncidenteCerradoEvent;
import com.conectatech.sgs_backend.model.Denunciante;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.conectatech.sgs_backend.util.PiiMaskingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final IncidenteRepository incidenteRepository;
    private final AuditoriaService auditoriaService;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.mail.username:sgs.eltabo.soporte@gmail.com}")
    private String remitente;

    @Value("${brevo.api.key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void sendPasswordResetEmail(String to, String token) {
        String resetUrl = "http://localhost:5173/recuperar-clave?token=" + token;
        String asunto = "SGS El Tabo - Recuperación de Contraseña";
        String contenidoTexto = "Hola,\n\nHas solicitado restablecer tu contraseña para el Sistema de Gestión de Seguridad (SGS).\n\n"
                +
                "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                resetUrl + "\n\n" +
                "Este enlace expirará en 15 minutos por motivos de seguridad.\n" +
                "Si no solicitaste esto, puedes ignorar este correo.";

        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            try {
                Map<String, Object> payload = Map.of(
                        "sender", Map.of("name", "SGSP El Tabo", "email", remitente),
                        "to", List.of(Map.of("email", to)),
                        "subject", asunto,
                        "textContent", contenidoTexto);
                String jsonBody = objectMapper.writeValueAsString(payload);
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("api-key", brevoApiKey.trim())
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();
                client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Correo de restablecimiento enviado vía Brevo API hacia {}", PiiMaskingUtils.maskEmail(to));
                return;
            } catch (Exception e) {
                log.warn("Fallo al enviar correo de restablecimiento vía Brevo, reintentando vía SMTP: {}",
                        e.getMessage());
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(to);
        message.setSubject(asunto);
        message.setText(contenidoTexto);
        mailSender.send(message);
        log.info("Correo de restablecimiento enviado vía SMTP hacia {}", PiiMaskingUtils.maskEmail(to));
    }

    /**
     * Listener asíncrono para eventos de cierre de incidente.
     * Despacha el correo en segundo plano sin bloquear la transacción HTTP.
     */
    @Async("taskExecutor")
    @EventListener
    public void manejarIncidenteCerrado(IncidenteCerradoEvent event) {
        if (event == null || event.getIncidente() == null) {
            return;
        }
        enviarCorreoResolucionCierre(event.getIncidente(), event.getResolucionMunicipal());
    }

    /**
     * Construye y envía el correo institucional con el reporte cerrado al vecino
     * denunciante.
     * Soporta despacho híbrido:
     * - Si está configurada BREVO_API_KEY: Despacha vía HTTP REST (puerto 443,
     * ideal para Render / Cloud sin bloqueos).
     * - Si no está configurada: Despacha vía SMTP tradicional (puerto 587, ideal
     * para despliegues on-premise en El Tabo).
     */
    public void enviarCorreoResolucionCierre(Incidente incidente, String resolucionMunicipal) {
        String folio = incidente.getCodigoCorrelativo() != null
                ? incidente.getCodigoCorrelativo()
                : ("#" + incidente.getId());

        Denunciante denunciante = incidente.getDenunciante();
        if (denunciante == null || denunciante.getCorreoElectronico() == null
                || denunciante.getCorreoElectronico().isBlank()) {
            log.warn("Incidente {} no cuenta con correo electrónico de denunciante para notificación.", folio);
            return;
        }

        String destinatario = denunciante.getCorreoElectronico().trim();
        String nombreVecino = "Vecino/a";
        if (denunciante.getNombres() != null && !denunciante.getNombres().isBlank()) {
            nombreVecino = denunciante.getNombres().trim();
            if (denunciante.getPrimerApellido() != null && !denunciante.getPrimerApellido().isBlank()) {
                nombreVecino += " " + denunciante.getPrimerApellido().trim();
            }
        }

        String tipoIncidente = incidente.getTipo() != null ? incidente.getTipo() : "Reporte de Seguridad";
        String fechaIngresoStr = incidente.getFechaCreacion() != null
                ? incidente.getFechaCreacion().format(FORMATO_FECHA_HORA)
                : LocalDateTime.now().format(FORMATO_FECHA_HORA);

        String resolucionTexto = (resolucionMunicipal != null && !resolucionMunicipal.isBlank())
                ? resolucionMunicipal.trim()
                : "Procedimiento finalizado y adoptado conforme a los protocolos operativos.";

        String asunto = "SGSP El Tabo Actualización de su reporte N° " + folio;

        Resource logoResource = buscarRecursoLogo();
        boolean tieneLogo = logoResource != null && logoResource.exists();

        String contenidoHtml = construirHtmlCierre(
                nombreVecino,
                folio,
                tipoIncidente,
                fechaIngresoStr,
                resolucionTexto,
                tieneLogo);

        boolean usarBrevo = brevoApiKey != null && !brevoApiKey.isBlank();

        try {
            if (usarBrevo) {
                log.info(
                        "Despachando correo institucional vía Brevo REST API (HTTPS puerto 443) hacia {} para el incidente {}...",
                        PiiMaskingUtils.maskEmail(destinatario), folio);
                enviarViaBrevo(destinatario, nombreVecino, asunto, contenidoHtml, logoResource, tieneLogo);
            } else {
                log.info(
                        "Despachando correo institucional vía SMTP estándar (puerto 587) hacia {} para el incidente {}...",
                        PiiMaskingUtils.maskEmail(destinatario), folio);
                enviarViaSmtp(destinatario, asunto, contenidoHtml, logoResource, tieneLogo);
            }

            log.info("Correo de notificación de cierre enviado exitosamente a {} para el incidente {}",
                    PiiMaskingUtils.maskEmail(destinatario), folio);

            // Registrar trazabilidad en el modelo
            incidente.setNotificadoCierreEmail(true);
            incidente.setFechaNotificacionCierre(LocalDateTime.now());
            incidenteRepository.save(incidente);

            // Registrar en auditoría forense
            String canal = usarBrevo ? "API Brevo (HTTPS)" : "SMTP Gmail (587)";
            auditoriaService.registrarAuditoria(
                    incidente.getId(),
                    folio,
                    null,
                    "Notificación por Correo",
                    "Pendiente",
                    "Enviado",
                    "Notificación de cierre remitida con éxito a " + PiiMaskingUtils.maskEmail(destinatario) + " ["
                            + canal + "]");

        } catch (Exception e) {
            log.error("Fallo al despachar correo de notificación de cierre para el incidente {}: {}", folio,
                    e.getMessage(), e);

            auditoriaService.registrarAuditoria(
                    incidente.getId(),
                    folio,
                    null,
                    "Notificación por Correo",
                    "Pendiente",
                    "Error",
                    "Fallo al enviar correo a " + PiiMaskingUtils.maskEmail(destinatario) + ": " + e.getMessage());
        }
    }

    /**
     * Envío transaccional vía Brevo REST API (HTTPS puerto 443).
     * Ideal para nubes PaaS como Render donde los puertos SMTP 25, 465 y 587 están
     * bloqueados por cortafuegos.
     */
    private void enviarViaBrevo(String destinatario, String nombreVecino, String asunto, String contenidoHtml,
            Resource logoResource, boolean tieneLogo) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of(
                "name", "I. Municipalidad de El Tabo - SGSP",
                "email", remitente));
        payload.put("to", List.of(Map.of(
                "email", destinatario,
                "name", nombreVecino)));
        payload.put("subject", asunto);
        payload.put("htmlContent", contenidoHtml);

        if (tieneLogo && logoResource != null) {
            try {
                byte[] bytes = logoResource.getInputStream().readAllBytes();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                payload.put("attachment", List.of(Map.of(
                        "name", "logoEscudo.png",
                        "content", base64)));
            } catch (Exception exLogo) {
                log.warn("No se pudo adjuntar el logo para Brevo API: {}", exLogo.getMessage());
            }
        }

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("api-key", brevoApiKey.trim())
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Brevo API HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Envío tradicional vía JavaMailSender (SMTP directo).
     */
    private void enviarViaSmtp(String destinatario, String asunto, String contenidoHtml, Resource logoResource,
            boolean tieneLogo) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(remitente);
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(contenidoHtml, true);

        if (tieneLogo) {
            try {
                String filename = logoResource.getFilename() != null ? logoResource.getFilename().toLowerCase() : "";
                String mimeType = filename.endsWith(".svg") ? "image/svg+xml" : "image/png";
                helper.addInline("logoEscudo.png", logoResource, mimeType);
            } catch (Exception exLogo) {
                log.warn("No se pudo incrustar el recurso de logo como inline CID, continuando envío: {}",
                        exLogo.getMessage());
            }
        }

        mailSender.send(mimeMessage);
    }

    /**
     * Intenta resolver el recurso del escudo institucional desde ubicaciones
     * típicas en assets.
     */
    private Resource buscarRecursoLogo() {
        String[] posiblesRutas = {
                "classpath:static/assets/Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:static/assets/Escudo_de_Armas_de_El_Tabo.png",
                "classpath:assets/Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:assets/Escudo_de_Armas_de_El_Tabo.png",
                "classpath:Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:Escudo_de_Armas_de_El_Tabo.png",
                "classpath:logo-el-tabo.png",
                "classpath:static/logo-el-tabo.png"
        };
        for (String ruta : posiblesRutas) {
            try {
                Resource res = resourceLoader.getResource(ruta);
                if (res != null && res.exists()) {
                    return res;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Construcción de plantilla HTML compatible con clientes de correo modernos.
     */
    private String construirHtmlCierre(
            String nombreVecino,
            String folio,
            String tipoIncidente,
            String fechaIngresoStr,
            String resolucionTexto,
            boolean tieneLogo) {
        String logoHtml = tieneLogo
                ? "<img src=\"cid:logoEscudo.png\" alt=\"Escudo de El Tabo\" style=\"max-height: 80px; width: auto; margin-bottom: 12px;\" />"
                : "<div style=\"font-size: 32px; margin-bottom: 8px;\">🏛️</div>";

        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Actualización de su reporte</title>
                </head>
                <body style="margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; line-height: 1.5;">
                    <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f1f5f9; padding: 30px 15px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 580px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;">
                                    <!-- CABECERA INSTITUCIONAL -->
                                    <tr>
                                        <td align="center" style="padding: 32px 24px 20px 24px; background: linear-gradient(180deg, #f8fafc 0%%, #ffffff 100%%); border-bottom: 2px solid #0284c7;">
                                            %s
                                            <h1 style="margin: 0; font-size: 19px; font-weight: 800; color: #0f172a; letter-spacing: -0.02em;">I. MUNICIPALIDAD DE EL TABO</h1>
                                            <p style="margin: 4px 0 0 0; font-size: 13px; font-weight: 600; color: #0284c7; text-transform: uppercase; letter-spacing: 0.05em;">Dirección de Seguridad Pública</p>
                                        </td>
                                    </tr>

                                    <!-- CUERPO PRINCIPAL -->
                                    <tr>
                                        <td style="padding: 28px 28px 20px 28px;">
                                            <p style="margin: 0 0 16px 0; font-size: 15px; font-weight: 600; color: #334155;">
                                                Estimado/a %s:
                                            </p>
                                            <p style="margin: 0 0 24px 0; font-size: 14px; color: #64748b; line-height: 1.6;">
                                                Le informamos que su reporte de seguridad ciudadana ingresado a nuestro sistema ha concluido su ciclo operativo y ha sido formalmente cerrado por el equipo municipal.
                                            </p>

                                            <!-- FICHA DEL REPORTE -->
                                            <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0; margin-bottom: 24px;">
                                                <tr>
                                                    <td style="padding: 18px 20px;">
                                                        <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0">
                                                            <tr>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 600; color: #64748b; width: 42%%;">N° de Folio:</td>
                                                                <td style="padding: 6px 0; font-size: 14px; font-weight: 800; color: #0f172a;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 600; color: #64748b;">Tipo de Incidente:</td>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 600; color: #1e293b;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 600; color: #64748b;">Fecha y hora de ingreso:</td>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 500; color: #1e293b;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding: 6px 0; font-size: 13px; font-weight: 600; color: #64748b;">Estado:</td>
                                                                <td style="padding: 6px 0;">
                                                                    <span style="display: inline-block; background-color: #dcfce7; color: #15803d; font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 6px; border: 1px solid #bbf7d0;">
                                                                        Cerrado
                                                                    </span>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- SECCIÓN RESOLUCIÓN MUNICIPAL -->
                                            <div style="background-color: #f0fdf4; border-left: 4px solid #16a34a; border-radius: 8px; padding: 16px 18px; margin-bottom: 24px;">
                                                <div style="font-size: 12px; font-weight: 800; text-transform: uppercase; color: #166534; letter-spacing: 0.04em; margin-bottom: 6px;">
                                                    Resolución Municipal
                                                </div>
                                                <div style="font-size: 13px; color: #14532d; line-height: 1.6; white-space: pre-line;">
                                                    %s
                                                </div>
                                            </div>

                                            <p style="margin: 0; font-size: 13px; color: #64748b; line-height: 1.5;">
                                                Agradecemos su colaboración y compromiso con la seguridad y convivencia de nuestra comuna.
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- PIE DE PÁGINA -->
                                    <tr>
                                        <td style="padding: 20px 24px; background-color: #f8fafc; border-top: 1px solid #e2e8f0; text-align: center;">
                                            <p style="margin: 0 0 6px 0; font-size: 12px; font-weight: 600; color: #475569;">
                                                Sistema de Gestión de Seguridad Pública
                                            </p>
                                            <p style="margin: 0 0 8px 0; font-size: 11px; color: #94a3b8;">
                                                Municipalidad de El Tabo
                                            </p>
                                            <p style="margin: 0; font-size: 10px; color: #94a3b8; line-height: 1.4;">
                                                Este es un correo electrónico generado automáticamente. Por favor no responda directamente a este mensaje.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(
                        logoHtml,
                        nombreVecino,
                        folio,
                        tipoIncidente,
                        fechaIngresoStr,
                        resolucionTexto);
    }
}
