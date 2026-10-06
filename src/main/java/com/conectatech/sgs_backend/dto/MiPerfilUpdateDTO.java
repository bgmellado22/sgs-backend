package com.conectatech.sgs_backend.dto;

import lombok.Data;

@Data
public class MiPerfilUpdateDTO {
    private String nombreCompleto;
    private String email;
    private String password;
}
