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
    private String origen;
}
