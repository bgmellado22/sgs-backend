package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Notificacion;
import com.conectatech.sgs_backend.model.enums.EstadoNotificacion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    // Cargar campana de notificaciones por usuario o alertas globales
    List<Notificacion> findByUsuarioDestinoIdInOrderByFechaCreacionDesc(Collection<String> usuarioDestinoIds);

    List<Notificacion> findByUsuarioDestinoIdOrderByFechaCreacionDesc(String usuarioDestinoId);

    List<Notificacion> findByUsuarioDestinoIdIn(Collection<String> usuarioDestinoIds);

    // Para el badge rojo con el contador de mensajes sin leer
    long countByUsuarioDestinoIdInAndEstado(Collection<String> usuarioDestinoIds, EstadoNotificacion estado);

    long countByUsuarioDestinoIdAndEstado(String usuarioDestinoId, EstadoNotificacion estado);

    // Para obtener solo las notificaciones no leídas
    List<Notificacion> findByUsuarioDestinoIdInAndEstadoOrderByFechaCreacionDesc(Collection<String> usuarioDestinoIds,
            EstadoNotificacion estado);

    List<Notificacion> findByUsuarioDestinoIdAndEstadoOrderByFechaCreacionDesc(String usuarioDestinoId,
            EstadoNotificacion estado);
}
