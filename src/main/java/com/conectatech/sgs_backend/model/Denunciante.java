package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Denunciante {

    private String nombres;

    @Field("primer_apellido")
    private String primerApellido;

    @Field("segundo_apellido")
    private String segundoApellido;

    private String telefono;

    private String run;

    @Field("correo_electronico")
    private String correoElectronico;

    @Field("en_el_lugar")
    private Boolean enElLugar;
}
