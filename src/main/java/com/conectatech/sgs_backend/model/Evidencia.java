package com.conectatech.sgs_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidencia {
    private String id;
    private String nombre;
    private String tipo;
    private Long tamano;
    private String url;
    @Builder.Default
    private LocalDateTime fechaCarga = LocalDateTime.now();
}
