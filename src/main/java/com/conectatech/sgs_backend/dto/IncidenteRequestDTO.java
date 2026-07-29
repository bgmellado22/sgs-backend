package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class IncidenteRequestDTO {
    @NotBlank(message = "La categoría no puede estar vacía")
    private String categoria;

    @NotBlank(message = "El tipo de evento es obligatorio")
    private String tipo;

    @NotBlank(message = "La descripción de los hechos es obligatoria")
    @Size(min = 10, max = 1500, message = "La descripción debe tener entre 10 y 1500 caracteres")
    private String descripcion;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad;

    @NotBlank(message = "El origen es obligatorio")
    private String origen;

    @NotNull(message = "La latitud es obligatoria")
    @Min(value = -34, message = "Latitud fuera de rango (Fuera del sector de cobertura)")
    @Max(value = -33, message = "Latitud fuera de rango (Fuera del sector de cobertura)")
    private Double latitud;

    @NotNull(message = "La longitud es obligatoria")
    @Min(value = -72, message = "Longitud fuera de rango (Fuera del sector de cobertura)")
    @Max(value = -71, message = "Longitud fuera de rango (Fuera del sector de cobertura)")
    private Double longitud;

    @NotBlank(message = "La referencia descriptiva es obligatoria")
    @Size(min = 5, max = 200, message = "La referencia debe tener entre 5 y 200 caracteres")
    private String direccionTexto;
}
