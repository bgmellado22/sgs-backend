package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.enums.EstadoNotificacion;
import com.conectatech.sgs_backend.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {
    private final NotificacionRepository notificacionRepository;

    // Guardar nueva notificación en la base de datos
    public Notificacion crearNotificacion(Notificacion notificacion) {
        return notificacionRepository.save(notificacion);
    }

    // Obtener todo el historial de un operador específico ordenado por fecha de
    // creación descendente
    public List<Notificacion> obtenerNotificacionesPorUsuario(String usuarioDestinoId) {
        return notificacionRepository.findByUsuarioDestinoIdOrderByFechaCreacionDesc(usuarioDestinoId);
    }

    // Obtener el número para la campana de alertas en el NavBar
    public long contarNoLeidas(String usuarioDestinoId) {
        return notificacionRepository.countByUsuarioDestinoIdAndEstado(usuarioDestinoId, EstadoNotificacion.NO_LEIDA);
    }

    // Cambiar el estado cuando el usuario abre el menú de la campana
    public void marcarComoLeida(String notificacionId) {
        notificacionRepository.findById(notificacionId).ifPresent(notificacion -> {
            notificacion.setEstado(EstadoNotificacion.LEIDA);
            notificacionRepository.save(notificacion);
        });
    }
}
