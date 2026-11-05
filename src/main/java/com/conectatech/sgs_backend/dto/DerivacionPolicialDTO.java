package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerivacionPolicialDTO {

    private Long version;

    @NotBlank(message = "La unidad receptora de Carabineros es obligatoria.")
    private String destacamentoReceptor;

    @NotBlank(message = "El canal de enlace es obligatorio.")
    private String viaComunicacion;

    @NotBlank(message = "El folio policial es estrictamente obligatorio.")
    @Size(min = 4, max = 60, message = "El folio debe tener entre 4 y 60 caracteres.")
    @Pattern(
        regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ\\s\\-\\/\\#\\.\\_]{4,60}$",
        message = "El folio contiene caracteres no permitidos. Use solo letras, números y signos (- / # . _)."
    )
    private String folioDerivacion;

    @NotBlank(message = "Los antecedentes informados a Carabineros son obligatorios.")
    private String antecedentesInformados;
}

