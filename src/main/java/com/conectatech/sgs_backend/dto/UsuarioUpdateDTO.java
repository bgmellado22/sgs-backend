package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private RolUsuario rol;
}