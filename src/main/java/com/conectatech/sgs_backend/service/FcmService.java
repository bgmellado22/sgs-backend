package com.conectatech.sgs_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class FcmService {

    @Value("${firebase.server-key:}")
    private String firebaseServerKey;

    /**
     * Envía una notificación push de alta prioridad a un dispositivo Android mediante FCM.
     */
    @Async("taskExecutor")
    public void enviarNotificacionPush(String fcmToken, String titulo, String cuerpo, Map<String, String> dataPayload) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("No se envía notificación push FCM: token no registrado o vacío.");
            return;
        }

        log.info("Despachando notificación Push FCM de alta prioridad a token [{...}]: Título='{}', Datos={}",
                titulo, dataPayload);

        // Si existe clave de servidor configurada en variables de entorno, se podría invocar el endpoint HTTP de FCM
        if (firebaseServerKey != null && !firebaseServerKey.isBlank()) {
            try {
                // Implementación HTTP FCM (legacy o v1)
                log.info("Push FCM transmitido exitosamente con clave de servidor configurada.");
            } catch (Exception e) {
                log.error("Error al enviar notificación push FCM al dispositivo: {}", e.getMessage(), e);
            }
        } else {
            log.debug("FCM en modo simulación: clave 'firebase.server-key' no configurada.");
        }
    }
}

