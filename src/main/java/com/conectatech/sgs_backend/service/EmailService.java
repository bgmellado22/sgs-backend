package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.event.IncidenteCerradoEvent;
import com.conectatech.sgs_backend.model.Denunciante;
import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import com.conectatech.sgs_backend.util.PiiMaskingUtils;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final IncidenteRepository incidenteRepository;
    private final AuditoriaService auditoriaService;
    private final ResourceLoader resourceLoader;

    @Value("${spring.mail.username:sgs.eltabo.soporte@gmail.com}")
    private String remitente;

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(to);
        message.setSubject("SGS El Tabo - Recuperación de Contraseña");

        String resetUrl = "http://localhost:5173/recuperar-clave?token=" + token;

        message.setText(
                "Hola,\n\nHas solicitado restablecer tu contraseña para el Sistema de Gestión de Seguridad (SGS).\n\n" +
                        "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                        resetUrl + "\n\n" +
                        "Este enlace expirará en 15 minutos por motivos de seguridad.\n" +
                        "Si no solicitaste esto, puedes ignorar este correo.");

        mailSender.send(message);
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
     * Construye y envía el correo institucional con el reporte cerrado al vecino denunciante.
     */
    public void enviarCorreoResolucionCierre(Incidente incidente, String resolucionMunicipal) {
        String folio = incidente.getCodigoCorrelativo() != null
                ? incidente.getCodigoCorrelativo()
                : ("#" + incidente.getId());

        Denunciante denunciante = incidente.getDenunciante();
        if (denunciante == null || denunciante.getCorreoElectronico() == null || denunciante.getCorreoElectronico().isBlank()) {
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

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);

            // Búsqueda del recurso del logotipo institucional (SVG o PNG en assets)
            Resource logoResource = buscarRecursoLogo();
            boolean tieneLogo = logoResource != null && logoResource.exists();

            String contenidoHtml = construirHtmlCierre(
                    nombreVecino,
                    folio,
                    tipoIncidente,
                    fechaIngresoStr,
                    resolucionTexto,
                    tieneLogo
            );

            helper.setText(contenidoHtml, true);

            // Si el logo está presente en el classpath, se asocia como inline CID
            if (tieneLogo) {
                String filename = logoResource.getFilename() != null ? logoResource.getFilename().toLowerCase() : "";
                String mimeType = filename.endsWith(".svg") ? "image/svg+xml" : "image/png";
                helper.addInline("logoEscudo", logoResource, mimeType);
            }

            mailSender.send(mimeMessage);
            log.info("Correo de notificación de cierre enviado exitosamente a {} para el incidente {}",
                    PiiMaskingUtils.maskEmail(destinatario), folio);

            // Registrar trazabilidad en el modelo
            incidente.setNotificadoCierreEmail(true);
            incidente.setFechaNotificacionCierre(LocalDateTime.now());
            incidenteRepository.save(incidente);

            // Registrar en auditoría forense
            auditoriaService.registrarAuditoria(
                    incidente.getId(),
                    folio,
                    null,
                    "Notificación por Correo",
                    "Pendiente",
                    "Enviado",
                    "Notificación de cierre remitida con éxito al correo " + PiiMaskingUtils.maskEmail(destinatario)
            );

        } catch (Exception e) {
            log.error("Fallo al despachar correo de notificación de cierre para el incidente {}: {}", folio, e.getMessage(), e);

            auditoriaService.registrarAuditoria(
                    incidente.getId(),
                    folio,
                    null,
                    "Notificación por Correo",
                    "Pendiente",
                    "Error",
                    "Fallo al enviar correo a " + PiiMaskingUtils.maskEmail(destinatario) + ": " + e.getMessage()
            );
        }
    }

    /**
     * Intenta resolver el recurso del escudo institucional desde ubicaciones típicas en assets.
     */
    private Resource buscarRecursoLogo() {
        String[] posiblesRutas = {
                "classpath:static/assets/Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:static/assets/Escudo_de_Armas_de_El_Tabo.png",
                "classpath:assets/Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:assets/Escudo_de_Armas_de_El_Tabo.png",
                "classpath:Escudo_de_Armas_de_El_Tabo.svg",
                "classpath:Escudo_de_Armas_de_El_Tabo.png"
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
            boolean tieneLogo
    ) {
        String logoHtml = tieneLogo
                ? "<img src=\"cid:logoEscudo\" alt=\"Escudo de El Tabo\" style=\"max-height: 80px; width: auto; margin-bottom: 12px;\" />"
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
                                        Sistema de Gestión de Seguridad Pública (SGSP)
                                    </p>
                                    <p style="margin: 0 0 8px 0; font-size: 11px; color: #94a3b8;">
                                        Municipalidad de El Tabo &bull; Fono Central de Seguridad: 14XX
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
        """.formatted(
                logoHtml,
                nombreVecino,
                folio,
                tipoIncidente,
                fechaIngresoStr,
                resolucionTexto
        );
    }
}
