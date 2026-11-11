package com.conectatech.sgs_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoPatrullajeWS {
    private String tipoEvento; // CREADO, EN_SITIO, CERRADO, ACTUALIZADO
    private PatrullajeDTO patrullaje;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}

