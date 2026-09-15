package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Notificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificacionWebSocketService {
    // Plantilla para enrutar mensajes a los canales STOMP configurados
    private final SimpMessagingTemplate messagingTemplate;

    // Inyección del service de notificaciones
    private final NotificacionService notificacionService;

    // Difusión privada
    public void despacharNotificacionPrivada(Notificacion notificacion) {
        // Primero se guarda en MongoDB
        Notificacion guardada = notificacionService.crearNotificacion(notificacion);

        // Despachar notificación al usuario específico
        messagingTemplate.convertAndSendToUser(
                guardada.getUsuarioDestinoId(),
                "/queue/notificaciones",
                guardada);
    }

    // Difusión global
    public void despacharAlertaGlobal(Notificacion alerta) {
        // Persistencia
        Notificacion guardada = notificacionService.crearNotificacion(alerta);

        // Despachar al tópico público configurado en WebSocketConfig
        messagingTemplate.convertAndSend(
                "/topic/alertas",
                guardada);
    }
}
