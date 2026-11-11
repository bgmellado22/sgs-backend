package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelemetriaInspectorDTO {

    @NotBlank(message = "El ID del inspector es obligatorio")
    private String inspectorId;

    private String inspectorNombre;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitud;

    private Double precisionMetros;
    private Double velocidadKmh;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

