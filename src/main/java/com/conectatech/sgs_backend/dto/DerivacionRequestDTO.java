package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DerivacionRequestDTO {
    @NotBlank(message = "La entidad de derivación es obligatoria (CARABINEROS o MUNICIPALIDAD)")
    private String entidad;

    private String detalle;
    private String inspectorId;
    private String inspectorNombre;
    private String observacion;
}
