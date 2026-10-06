package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Incidente;
import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.enums.TipoNotificacion;
import com.conectatech.sgs_backend.repository.IncidenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaMonitorService {

    private final IncidenteRepository incidenteRepository;
    private final NotificacionWebSocketService websocketService;

    @Scheduled(fixedRate = 300000) // Se ejecuta cada 5 minutos
    public void monitorVencimientosSLA() {
        // Obtenemos todos los incidentes activos que no estén resueltos o cerrados
        List<Incidente> abiertos = incidenteRepository.findByEstadoNotIn(List.of("Resuelto", "Cerrado"));

        LocalDateTime ahora = LocalDateTime.now();

        for (Incidente inc : abiertos) {
            if (inc.getFechaVencimientoSla() != null) {
                // Si venció y la diferencia es menor a 5 minutos, despachamos alerta
                // (Esto evita spamear a cada rato con el mismo incidente viejo vencido)
                if (ahora.isAfter(inc.getFechaVencimientoSla()) && ahora.minusMinutes(5).isBefore(inc.getFechaVencimientoSla())) {
                    log.warn("SLA Vencido detectado para incidente {}", inc.getCodigoCorrelativo());

                    Notificacion alertaVencimiento = Notificacion.builder()
                            .titulo("¡SLA Vencido!")
                            .mensaje("El incidente " + inc.getCodigoCorrelativo() + " ha superado su límite de tiempo de respuesta.")
                            .tipo(TipoNotificacion.ALERTA)
                            .referenciaId(inc.getId())
                            .usuarioDestinoId("GLOBAL")
                            .build();

                    websocketService.despacharAlertaGlobal(alertaVencimiento);
                }
            }
        }
    }
}
