package com.conectatech.sgs_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

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
}
