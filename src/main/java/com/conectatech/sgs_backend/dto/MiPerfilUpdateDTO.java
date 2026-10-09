package com.conectatech.sgs_backend.dto;

import lombok.Data;

@Data
public class MiPerfilUpdateDTO {
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String email;
    private String password;
}
