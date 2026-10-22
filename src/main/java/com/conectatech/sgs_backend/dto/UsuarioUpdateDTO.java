package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    @Size(max = 80, message = "Los nombres no deben superar los 80 caracteres.")
    private String nombres;

    @Size(max = 80, message = "El primer apellido no debe superar los 80 caracteres.")
    private String primerApellido;

    @Size(max = 80, message = "El segundo apellido no debe superar los 80 caracteres.")
    private String segundoApellido;

    @Email(message = "Ingrese un correo electrónico válido.")
    private String email;

    private RolUsuario rol;
}