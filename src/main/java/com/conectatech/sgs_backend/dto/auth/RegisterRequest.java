package com.conectatech.sgs_backend.dto.auth;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import com.conectatech.sgs_backend.validation.RutChileno;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El RUN es obligatorio.")
    @RutChileno(message = "El RUN no es válido (formato o dígito verificador incorrecto).")
    private String run;

    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres.")
    private String nombres;

    @NotBlank(message = "El primer apellido es obligatorio.")
    @Size(min = 2, max = 80, message = "El primer apellido debe tener entre 2 y 80 caracteres.")
    private String primerApellido;

    @Size(max = 80, message = "El segundo apellido no debe superar los 80 caracteres.")
    private String segundoApellido;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingrese un correo electrónico válido (ej: usuario@dominio.cl).")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula, número y carácter especial."
    )
    private String password;

    private RolUsuario rol;
}
