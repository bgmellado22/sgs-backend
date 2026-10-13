package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.Evidencia;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class IncidenteResponseDTO {
    private String id;
    private String codigoCorrelativo;
    private String categoria;
    private String tipo;
    private String descripcion;
    private String prioridad;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaCierre;
    private Long tiempoResolucionMinutos;
    private Long slaMinutosObjetivo;
    private LocalDateTime fechaVencimientoSla;
    private String origen;
    private Double latitud;
    private Double longitud;
    private String direccionTexto;
    private String sector; // Localidad más cercana (calculada, no persistida)

    // Gestión Operativa (Derivación)
    private boolean derivado;
    private String derivacionEntidad;
    private String derivacionDetalle;
    private String derivacionInspectorId;
    private String derivacionInspectorNombre;
    private String derivacionObservacion;
    private LocalDateTime fechaDerivacion;
    private String derivadoPor;

    // Evidencias Multimedia
    private List<Evidencia> evidencias;
}
