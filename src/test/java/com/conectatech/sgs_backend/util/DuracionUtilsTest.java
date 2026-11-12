package com.conectatech.sgs_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuracionUtilsTest {

    @Test
    @DisplayName("Debe formatear segundos correctamente cuando es menor a 1 minuto")
    void testFormatearSegundosMenorUnMinuto() {
        assertEquals("0 seg", DuracionUtils.formatearSegundos(0));
        assertEquals("45 seg", DuracionUtils.formatearSegundos(45));
        assertEquals("59 seg", DuracionUtils.formatearSegundos(59));
    }

    @Test
    @DisplayName("Debe formatear minutos exactos y minutos con segundos")
    void testFormatearMinutos() {
        assertEquals("1 min", DuracionUtils.formatearSegundos(60));
        assertEquals("1m 15s", DuracionUtils.formatearSegundos(75));
        assertEquals("15 min", DuracionUtils.formatearSegundos(900));
        assertEquals("15m 30s", DuracionUtils.formatearSegundos(930));
    }

    @Test
    @DisplayName("Debe formatear horas exactas y horas con minutos")
    void testFormatearHoras() {
        assertEquals("1h", DuracionUtils.formatearSegundos(3600));
        assertEquals("2h 15m", DuracionUtils.formatearSegundos(8100));
        assertEquals("23h 59m", DuracionUtils.formatearSegundos(86340));
    }

    @Test
    @DisplayName("Debe formatear días y días con horas")
    void testFormatearDias() {
        assertEquals("1d", DuracionUtils.formatearSegundos(86400));
        assertEquals("1d 4h", DuracionUtils.formatearSegundos(100800));
        assertEquals("3d 12h", DuracionUtils.formatearSegundos(302400));
    }

    @Test
    @DisplayName("Debe formatear a partir de fechas LocalDateTime")
    void testFormatearDesdeFechas() {
        LocalDateTime inicio = LocalDateTime.of(2026, 5, 10, 10, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 5, 10, 10, 0, 45);

        assertEquals("45 seg", DuracionUtils.formatearTiempoResolucion(inicio, fin, null));

        LocalDateTime finHoras = LocalDateTime.of(2026, 5, 10, 12, 30, 0);
        assertEquals("2h 30m", DuracionUtils.formatearTiempoResolucion(inicio, finHoras, null));

        LocalDateTime finDias = LocalDateTime.of(2026, 5, 12, 14, 0, 0);
        assertEquals("2d 4h", DuracionUtils.formatearTiempoResolucion(inicio, finDias, null));
    }

    @Test
    @DisplayName("Debe manejar valores nulos y fallback de minutos correctamente")
    void testValoresNulosYFallback() {
        assertEquals("-", DuracionUtils.formatearTiempoResolucion(null, null, null));
        assertEquals("2h 15m", DuracionUtils.formatearTiempoResolucion(null, null, 135L));
        assertEquals("45 seg", DuracionUtils.formatearSegundos(45));
        assertEquals("-", DuracionUtils.formatearSegundos(-10));
    }
}

