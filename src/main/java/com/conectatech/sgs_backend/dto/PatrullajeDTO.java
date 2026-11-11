package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.EstadoPatrullaje;
import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatrullajeDTO {
    private String id;
    private String codigoCorrelativo;
    private Double latitud;
    private Double longitud;
    private String coordenadasTexto;
    private String sector;
    private String inspectorId;
    private String inspectorNombre;
    private String tipoPatrullaje;
    private String objetivo;
    private String instruccionesTacticas;
    private Integer tiempoEstimadoMinutos;
    private EstadoPatrullaje estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaLlegadaSitio;
    private LocalDateTime fechaCierre;
    private Long duracionEfectivaMinutos;
    private ResultadoProcedimiento resultadoProcedimiento;
    private String observacionesCierre;
    private String evidenciaFotoId;
    private String evidenciaUrl;
    private Boolean evidenciaDepurada;
    private Boolean activo;
}

