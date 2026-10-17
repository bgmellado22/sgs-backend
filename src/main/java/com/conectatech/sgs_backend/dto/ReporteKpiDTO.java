package com.conectatech.sgs_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteKpiDTO {
    private long volumenOperativoTotal;
    private double tasaResolucionEfectiva;
    private double cumplimientoTiempoRespuesta;
    private double indiceCriticidad;
    @Builder.Default
    private List<CategoriaDistribucion> distribucionCategorias = new ArrayList<>();

    public ReporteKpiDTO(long volumenOperativoTotal, double tasaResolucionEfectiva, double cumplimientoTiempoRespuesta, double indiceCriticidad) {
        this.volumenOperativoTotal = volumenOperativoTotal;
        this.tasaResolucionEfectiva = tasaResolucionEfectiva;
        this.cumplimientoTiempoRespuesta = cumplimientoTiempoRespuesta;
        this.indiceCriticidad = indiceCriticidad;
        this.distribucionCategorias = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDistribucion {
        private String valor;
        private String etiqueta;
        private String colorHex;
        private long cantidad;
        private Integer nivelOrden;
    }
}