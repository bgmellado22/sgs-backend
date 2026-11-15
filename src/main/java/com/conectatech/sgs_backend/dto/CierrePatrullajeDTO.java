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
public class CierrePatrullajeDTO {

    @NotBlank(message = "El resultado del procedimiento es obligatorio")
    private String resultadoProcedimiento;

    private String observaciones;
}

