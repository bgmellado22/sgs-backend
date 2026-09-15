package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.enums.EstadoNotificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    // Cargar campana de notificaciones (historial completo del usuario) ordenado
    // por fecha de creación descendente
    List<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(String usuarioDestinoId);

    // Para el badge rojo con el contador de mensajes sin leer
    long countByUsuarioDestinoIdAndEstado(String usuarioDestinoId, EstadoNotificacion estado);

    // Para obtener solo las notificaciones no leídas para un Toast al iniciar
    // sesión
    List<Notificacion> findByUsuarioDestinoIdAndEstadoOrderByFechaCreacionDesc(String usuarioDestinoId,
            EstadoNotificacion estado);
}
