package com.conectatech.sgs_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReporteFormatoUtilsTest {

    @Test
    @DisplayName("Debe formatear origen CAMARA_SEGURIDAD a Cámara de Seguridad sin uppercases")
    void testFormatearOrigenCamaraSeguridad() {
        assertEquals("Cámara de Seguridad", ReporteFormatoUtils.formatearOrigen("CAMARA_SEGURIDAD"));
        assertEquals("Llamada Telefónica", ReporteFormatoUtils.formatearOrigen("LLAMADA"));
        assertEquals("Patrulla en Terreno", ReporteFormatoUtils.formatearOrigen("PATRULLA"));
        assertEquals("Atención Presencial", ReporteFormatoUtils.formatearOrigen("PRESENCIAL"));
        assertEquals("App Más Seguridad", ReporteFormatoUtils.formatearOrigen("APP_MAS_SEGURIDAD"));
    }

    @Test
    @DisplayName("Debe formatear categorías estándar y preservar SOS")
    void testFormatearCategoria() {
        assertEquals("Incivilidad", ReporteFormatoUtils.formatearCategoria("INCIVILIDAD"));
        assertEquals("Delito", ReporteFormatoUtils.formatearCategoria("DELITO"));
        assertEquals("SOS", ReporteFormatoUtils.formatearCategoria("SOS"));
    }

    @Test
    @DisplayName("Debe formatear prioridades estándar")
    void testFormatearPrioridad() {
        assertEquals("Alta", ReporteFormatoUtils.formatearPrioridad("ALTA"));
        assertEquals("Media", ReporteFormatoUtils.formatearPrioridad("MEDIA"));
        assertEquals("Baja", ReporteFormatoUtils.formatearPrioridad("BAJA"));
    }

    @Test
    @DisplayName("Debe formatear estados operativos")
    void testFormatearEstado() {
        assertEquals("Pendiente", ReporteFormatoUtils.formatearEstado("PENDIENTE"));
        assertEquals("En Proceso", ReporteFormatoUtils.formatearEstado("EN_PROCESO"));
        assertEquals("Resuelto", ReporteFormatoUtils.formatearEstado("RESUELTO"));
        assertEquals("Cerrado", ReporteFormatoUtils.formatearEstado("CERRADO"));
        assertEquals("Derivado", ReporteFormatoUtils.formatearEstado("DERIVADO"));
        assertEquals("Cancelado", ReporteFormatoUtils.formatearEstado("CANCELADO"));
    }

    @Test
    @DisplayName("Debe formatear tipos de incidente conocidos")
    void testFormatearTipo() {
        assertEquals("Robo", ReporteFormatoUtils.formatearTipo("ROBO_HURTO"));
        assertEquals("Incendio", ReporteFormatoUtils.formatearTipo("INCENDIO"));
        assertEquals("Violencia Intrafamiliar (VIF)", ReporteFormatoUtils.formatearTipo("VIF"));
        assertEquals("Accidente de Tránsito", ReporteFormatoUtils.formatearTipo("ACCIDENTE_TRANSITO"));
    }

    @Test
    @DisplayName("Debe priorizar etiquetas de catálogo dinámico sobre valores por defecto")
    void testPriorizarCatalogoDinamico() {
        Map<String, String> dinamico = new HashMap<>();
        dinamico.put("CAMARA_SEGURIDAD", "Cámara de Vigilancia Municipal");
        dinamico.put("TIPO_PERSONALIZADO", "Inspección de Obras");

        assertEquals("Cámara de Vigilancia Municipal", ReporteFormatoUtils.formatearOrigen("CAMARA_SEGURIDAD", dinamico));
        assertEquals("Inspección de Obras", ReporteFormatoUtils.formatearTipo("TIPO_PERSONALIZADO", dinamico));
    }

    @Test
    @DisplayName("Debe capitalizar strings desconocidos en mayúsculas o snake_case a Title Case")
    void testCapitalizarTextoGenerico() {
        assertEquals("Patrullaje Preventivo", ReporteFormatoUtils.capitalizarTexto("PATRULLAJE_PREVENTIVO"));
        assertEquals("Camara Sector Norte", ReporteFormatoUtils.capitalizarTexto("CAMARA_SECTOR_NORTE"));
        assertEquals("-", ReporteFormatoUtils.capitalizarTexto(null));
        assertEquals("-", ReporteFormatoUtils.capitalizarTexto("   "));
    }
}
