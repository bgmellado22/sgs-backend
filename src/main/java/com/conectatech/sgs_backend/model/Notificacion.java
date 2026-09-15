package com.conectatech.sgs_backend.model;

import com.conectatech.sgs_backend.model.enums.EstadoNotificacion;
import com.conectatech.sgs_backend.model.enums.TipoNotificacion;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notificaciones")
public class Notificacion {
    @Id
    private String id;

    private String titulo;
    private String mensaje;
    private TipoNotificacion tipo;

    @Builder.Default
    private EstadoNotificacion estado = EstadoNotificacion.NO_LEIDA;

    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // lógica de: a quién va dirigida la notificación?
    private String usuarioDestinoId;

    // Si la notificación nace de un evento específico (ej. un nuevo incidente)
    private String referenciaId;
}
