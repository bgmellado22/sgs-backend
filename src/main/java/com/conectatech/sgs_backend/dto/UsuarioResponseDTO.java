package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResponseDTO {
    private String id;
    private String run;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String nombreCompleto;
    private String email;
    private RolUsuario rol;
    private Boolean estado;
}