package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.ResultadoProcedimiento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierrePatrullajeDTO {

    @NotNull(message = "El resultado del procedimiento es obligatorio")
    private ResultadoProcedimiento resultadoProcedimiento;

    private String observaciones;
}

