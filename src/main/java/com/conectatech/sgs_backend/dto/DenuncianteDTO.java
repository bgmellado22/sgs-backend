package com.conectatech.sgs_backend.dto;

import com.conectatech.sgs_backend.validation.RutChileno;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DenuncianteDTO {
    @Size(max = 80, message = "Los nombres no deben superar los 80 caracteres.")
    private String nombres;

    @Size(max = 80, message = "El primer apellido no debe superar los 80 caracteres.")
    private String primerApellido;

    @Size(max = 80, message = "El segundo apellido no debe superar los 80 caracteres.")
    private String segundoApellido;

    @Pattern(
        regexp = "^$|^\\+?[0-9\\s\\-]{8,15}$",
        message = "Ingrese un número de teléfono de contacto válido (ej: +56 9 1234 5678)."
    )
    private String telefono;

    @RutChileno(message = "El RUN del denunciante no es válido (formato o dígito verificador incorrecto).")
    private String run;

    @Email(message = "Ingrese un correo electrónico válido para el denunciante.")
    private String correoElectronico;

    private Boolean enElLugar;
}
