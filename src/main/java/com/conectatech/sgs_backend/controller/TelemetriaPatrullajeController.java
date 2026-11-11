package com.conectatech.sgs_backend.controller;

import com.conectatech.sgs_backend.dto.TelemetriaInspectorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TelemetriaPatrullajeController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Canal STOMP de entrada para telemetría continua de inspectores (/app/telemetria-inspector).
     * Retransmite en tiempo real a los operadores tácticos suscritos a /topic/telemetria-inspectores.
     */
    @MessageMapping("/telemetria-inspector")
    public void recibirTelemetriaInspector(TelemetriaInspectorDTO telemetria) {
        log.debug("Telemetría recibida para inspector [{}]: Lat={}, Lng={}",
                telemetria.getInspectorId(), telemetria.getLatitud(), telemetria.getLongitud());
        messagingTemplate.convertAndSend("/topic/telemetria-inspectores", telemetria);
    }
}

