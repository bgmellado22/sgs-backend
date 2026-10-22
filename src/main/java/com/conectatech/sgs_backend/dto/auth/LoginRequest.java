package com.conectatech.sgs_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingrese un correo electrónico válido.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;
}