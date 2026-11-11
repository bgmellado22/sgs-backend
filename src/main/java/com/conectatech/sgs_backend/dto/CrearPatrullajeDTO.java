package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPatrullajeDTO {

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "Latitud inválida")
    @DecimalMax(value = "90.0", message = "Latitud inválida")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "Longitud inválida")
    @DecimalMax(value = "180.0", message = "Longitud inválida")
    private Double longitud;

    private String coordenadasTexto;

    @NotBlank(message = "El sector es obligatorio")
    private String sector;

    @NotBlank(message = "El inspector asignado es obligatorio")
    private String inspectorId;

    private String inspectorNombre;

    @NotBlank(message = "El tipo de patrullaje es obligatorio")
    private String tipoPatrullaje;

    @NotBlank(message = "El objetivo de la patrulla es obligatorio")
    private String objetivo;

    private String instruccionesTacticas;

    @NotNull(message = "El tiempo estimado de patrullaje es obligatorio")
    @Min(value = 1, message = "El tiempo estimado debe ser de al menos 1 minuto")
    private Integer tiempoEstimadoMinutos;
}

