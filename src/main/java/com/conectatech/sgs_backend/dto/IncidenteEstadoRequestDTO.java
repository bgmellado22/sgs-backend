package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidenteEstadoRequestDTO {

    private Long version;

    @NotBlank(message = "El nuevo estado no puede estar vacío")
    private String estado;

    @Size(max = 2000, message = "El motivo no puede exceder los 2000 caracteres")
    private String motivo;

    @Size(max = 2000, message = "La resolución municipal no puede superar los 2000 caracteres")
    private String resolucionMunicipal;

    private Boolean notificarDenunciante;
}

