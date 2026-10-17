package com.conectatech.sgs_backend.dto;

import lombok.Data;

@Data
public class MiPerfilUpdateDTO {
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String email;
    private String password;
}
