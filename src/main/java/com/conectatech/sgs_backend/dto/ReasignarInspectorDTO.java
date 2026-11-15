package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasignarInspectorDTO {

    @NotBlank(message = "El nuevo inspector es obligatorio")
    private String nuevoInspectorId;

    private String motivo;

    private Long version;
}

