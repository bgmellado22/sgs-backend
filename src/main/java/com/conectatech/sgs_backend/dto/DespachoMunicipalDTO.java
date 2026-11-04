package com.conectatech.sgs_backend.dto;

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
public class DespachoMunicipalDTO {
    private String inspectorId;
    private String inspectorNombre;

    @NotNull(message = "Debe especificar el tipo de patrullaje (MUNICIPAL u OS14).")
    @Builder.Default
    private String tipoPatrullaje = "MUNICIPAL";

    @NotBlank(message = "Las instrucciones de despacho preventivo son obligatorias.")
    private String instrucciones;
}

