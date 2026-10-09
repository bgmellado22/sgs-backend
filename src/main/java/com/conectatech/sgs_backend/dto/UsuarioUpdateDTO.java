package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private RolUsuario rol;
}