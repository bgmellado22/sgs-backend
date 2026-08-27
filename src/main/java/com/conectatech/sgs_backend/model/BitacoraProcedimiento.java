package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bitacora_procedimientos")
public class BitacoraProcedimiento {

    @Id
    private String id;

    // Relación lógica con el Incidente
    private String incidenteId;

    // Relación lógica con el Usuario que ejecutó el cambio
    private String usuarioId;

    // Para auditoría visual en el Frontend
    private String nombreActor;
    private String rolActor;

    // Trazabilidad del cambio de estado
    private String estadoAnterior;
    private String estadoNuevo;

    // Marca de tiempo inalterable
    @Builder.Default
    private LocalDateTime fechaModificacion = LocalDateTime.now();

    // Si el operador desea agregar una nota al cambiar el estado
    private String comentario;
}