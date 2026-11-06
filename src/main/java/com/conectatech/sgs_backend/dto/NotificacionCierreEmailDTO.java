package com.conectatech.sgs_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionCierreEmailDTO {
    private String destinatarioEmail;
    private String nombreDestinatario;
    private String codigoCorrelativo;
    private String tipoIncidente;
    private String fechaIngresoFormateada;
    private String resolucionMunicipal;
    private String asunto;
}

