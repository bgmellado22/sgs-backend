package com.conectatech.sgs_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "El token de recuperación es obligatorio.")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{8,}$",
        message = "La nueva contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula, número y carácter especial."
    )
    private String newPassword;
}
