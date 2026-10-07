package com.conectatech.sgs_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEstadisticasDTO {

    // ── Volumen y tendencia (últimos 7 días vs. los 7 anteriores) ──
    private long totalIncidentes;
    private long incidentesSemanaActual;
    private long incidentesSemanaAnterior;
    private Double variacionPorcentual; // null si la semana anterior no tuvo incidentes

    // ── Estados y tasa de resolución (histórico) ──
    private long pendientes;
    private long enProceso;
    private long cerrados; // Resuelto + Cerrado
    private double tasaResolucion;

    // ── Series para gráficos (últimos 7 días) ──
    private List<TendenciaDiaria> tendenciaSemanal;
    private List<FranjaHoraria> distribucionHoraria;
    private FranjaHoraria franjaCritica; // null si no hay incidentes en la semana
    private List<Hotspot> hotspots;

    // ── Distribución por categoría (histórico) ──
    private List<CategoriaDistribucion> distribucionCategorias;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendenciaDiaria {
        private LocalDate fecha;
        private String dia; // Ej: "Lun"
        private long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FranjaHoraria {
        private String franja;   // MADRUGADA, MANANA, TARDE, NOCHE
        private String etiqueta; // Ej: "Noche"
        private String rango;    // Ej: "18:00 - 00:00"
        private long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hotspot {
        private String sector;
        private long cantidad;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDistribucion {
        private String valor;
        private String etiqueta;
        private String colorHex;
        private long cantidad;
    }
}
