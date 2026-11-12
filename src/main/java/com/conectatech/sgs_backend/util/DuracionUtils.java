package com.conectatech.sgs_backend.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Utilidades para cálculo y formateo de duraciones de tiempo (SGS El Tabo).
 * Permite presentar tiempos de resolución de forma humana y legible
 * considerando segundos, minutos, horas y días.
 */
public final class DuracionUtils {

    private DuracionUtils() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Formatea el tiempo de resolución transcurrido entre dos marcas temporales.
     * Si alguna fecha falta, utiliza el parámetro minutosFallback como respaldo.
     *
     * @param inicio Fecha y hora de creación/inicio
     * @param fin Fecha y hora de cierre/resolución
     * @param minutosFallback Minutos precalculados en caso de fechas ausentes
     * @return Cadena formateada amigable (ej: "45 seg", "12m 30s", "2h 15m", "1d 4h") o "-"
     */
    public static String formatearTiempoResolucion(LocalDateTime inicio, LocalDateTime fin, Long minutosFallback) {
        if (inicio != null && fin != null) {
            Duration duracion = Duration.between(inicio, fin);
            if (!duracion.isNegative()) {
                return formatearSegundos(duracion.getSeconds());
            }
        }

        if (minutosFallback != null && minutosFallback >= 0) {
            return formatearSegundos(minutosFallback * 60);
        }

        return "-";
    }

    /**
     * Formatea una cantidad de segundos a representación legible adaptativa.
     *
     * @param totalSegundos Cantidad total de segundos
     * @return Cadena formateada
     */
    public static String formatearSegundos(long totalSegundos) {
        if (totalSegundos < 0) {
            return "-";
        }
        if (totalSegundos < 60) {
            return totalSegundos + " seg";
        }

        long dias = totalSegundos / 86400;
        long restoHoras = (totalSegundos % 86400) / 3600;
        long restoMinutos = (totalSegundos % 3600) / 60;
        long restoSegundos = totalSegundos % 60;

        if (dias > 0) {
            return restoHoras > 0 ? String.format("%dd %dh", dias, restoHoras) : String.format("%dd", dias);
        }
        if (restoHoras > 0) {
            return restoMinutos > 0 ? String.format("%dh %dm", restoHoras, restoMinutos) : String.format("%dh", restoHoras);
        }
        if (restoMinutos > 0) {
            return restoSegundos > 0 ? String.format("%dm %ds", restoMinutos, restoSegundos) : String.format("%d min", restoMinutos);
        }
        return totalSegundos + " seg";
    }
}

