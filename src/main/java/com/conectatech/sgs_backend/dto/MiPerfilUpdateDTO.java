package com.conectatech.sgs_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MiPerfilUpdateDTO {
    @Size(max = 80, message = "Los nombres no deben superar los 80 caracteres.")
    private String nombres;

    @Size(max = 80, message = "El primer apellido no debe superar los 80 caracteres.")
    private String primerApellido;

    @Size(max = 80, message = "El segundo apellido no debe superar los 80 caracteres.")
    private String segundoApellido;

    @Email(message = "Ingrese un correo electrónico válido.")
    private String email;

    @Pattern(
        regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{8,}$",
        message = "La nueva contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula, número y carácter especial."
    )
    private String password;
}
