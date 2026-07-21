package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IncidenteRequestDTO {
    @NotBlank(message = "La categoría no puede estar vacía")
    private String categoria;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipo;

    @NotBlank(message = "La descripción de los hechos es obligatoria")
    private String descripcion;

    private String prioridad;
    private String origen;

    @NotNull(message = "La latitud es obligatoria")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    private Double longitud;

    private String direccionTexto;
}
