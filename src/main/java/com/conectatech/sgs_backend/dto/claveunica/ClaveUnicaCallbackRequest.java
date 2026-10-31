package com.conectatech.sgs_backend.dto.claveunica;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaveUnicaCallbackRequest {

    @NotBlank(message = "El código de autorización 'code' es obligatorio.")
    private String code;

    @NotBlank(message = "El parámetro anti-falsificación 'state' es obligatorio.")
    private String state;

    @Builder.Default
    private String canal = "WEB"; // "WEB" o "MOVIL"
}

