package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nombreCompleto;
    private String email;
    private RolUsuario rol;
}