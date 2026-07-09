package com.conectatech.sgs_backend.dto.auth;

import com.conectatech.sgs_backend.model.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String rut;
    private String nombreCompleto;
    private String email;
    private String password;
    private RolUsuario rol;
}
