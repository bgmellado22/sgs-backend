package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    @NotBlank(message = "Los nombres son obligatorios.")
    @Size(min = 2, max = 80, message = "Los nombres deben tener entre 2 y 80 caracteres.")
    private String nombres;

    @NotBlank(message = "El primer apellido es obligatorio.")
    @Size(min = 2, max = 80, message = "El primer apellido debe tener entre 2 y 80 caracteres.")
    private String primerApellido;

    @NotBlank(message = "El segundo apellido es obligatorio.")
    @Size(min = 2, max = 80, message = "El segundo apellido debe tener entre 2 y 80 caracteres.")
    private String segundoApellido;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingrese un correo electrónico válido.")
    private String email;

    private RolUsuario rol;
}
