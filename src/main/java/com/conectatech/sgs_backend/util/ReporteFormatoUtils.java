package com.conectatech.sgs_backend.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utilidades para formateo y presentación de datos en reportes analíticos y ejecutivos (Excel y PDF).
 * Normaliza códigos, identificadores y enums a etiquetas legibles en español, eliminando
 * mayúsculas sostenidas y formatos técnicos (ej: CAMARA_SEGURIDAD -> Cámara de Seguridad).
 */
public final class ReporteFormatoUtils {

    private static final Map<String, String> ORIGENES_ESTANDAR;
    private static final Map<String, String> CATEGORIAS_ESTANDAR;
    private static final Map<String, String> PRIORIDADES_ESTANDAR;
    private static final Map<String, String> ESTADOS_ESTANDAR;
    private static final Map<String, String> TIPOS_ESTANDAR;

    private static final Set<String> CONECTORES = Set.of(
            "de", "del", "la", "las", "el", "los", "en", "a", "y", "o", "por", "con", "al"
    );

    private static final Set<String> SIGLAS = Set.of(
            "SOS", "VIF", "CENCO", "RUN", "RUT", "SGS", "SGSP", "PDF", "SLA", "GPS", "ID"
    );

    static {
        Map<String, String> origenes = new HashMap<>();
        origenes.put("CAMARA_SEGURIDAD", "Cámara de Seguridad");
        origenes.put("LLAMADA", "Llamada Telefónica");
        origenes.put("PATRULLA", "Patrulla en Terreno");
        origenes.put("PRESENCIAL", "Atención Presencial");
        origenes.put("APP_MAS_SEGURIDAD", "App Más Seguridad");
        ORIGENES_ESTANDAR = Collections.unmodifiableMap(origenes);

        Map<String, String> categorias = new HashMap<>();
        categorias.put("SOS", "SOS");
        categorias.put("DELITO", "Delito");
        categorias.put("INCIVILIDAD", "Incivilidad");
        CATEGORIAS_ESTANDAR = Collections.unmodifiableMap(categorias);

        Map<String, String> prioridades = new HashMap<>();
        prioridades.put("BAJA", "Baja");
        prioridades.put("MEDIA", "Media");
        prioridades.put("ALTA", "Alta");
        PRIORIDADES_ESTANDAR = Collections.unmodifiableMap(prioridades);

        Map<String, String> estados = new HashMap<>();
        estados.put("PENDIENTE", "Pendiente");
        estados.put("EN_PROCESO", "En Proceso");
        estados.put("EN_SITIO", "En Sitio");
        estados.put("RESUELTO", "Resuelto");
        estados.put("CERRADO", "Cerrado");
        estados.put("DERIVADO", "Derivado");
        estados.put("CANCELADO", "Cancelado");
        estados.put("ABIERTO", "Abierto");
        estados.put("FINALIZADO", "Finalizado");
        estados.put("EN_GESTION", "En Gestión");
        estados.put("EN_GESTIÓN", "En Gestión");
        ESTADOS_ESTANDAR = Collections.unmodifiableMap(estados);

        Map<String, String> tipos = new HashMap<>();
        tipos.put("ROBO_HURTO", "Robo");
        tipos.put("INCENDIO", "Incendio");
        tipos.put("VIF", "Violencia Intrafamiliar (VIF)");
        tipos.put("CONSUMO_DROGAS", "Consumo de Alcohol/Drogas en Vía Pública");
        tipos.put("RUIDOS_MOLESTOS", "Ruidos Molestos");
        tipos.put("ACCIDENTE_TRANSITO", "Accidente de Tránsito");
        tipos.put("SOSPECHOSO", "Persona Sospechosa");
        tipos.put("COMERCIO_AMBULANTE", "Comercio Ambulante no Autorizado");
        tipos.put("OTRO", "Otro");
        TIPOS_ESTANDAR = Collections.unmodifiableMap(tipos);
    }

    private ReporteFormatoUtils() {
        // Clase utilitaria, no instanciable
    }

    /**
     * Formatea un valor de origen de denuncia resolviendo catálogos dinámicos o valores estándar.
     */
    public static String formatearOrigen(String origen, Map<String, String> catalogoMap) {
        return resolverValor(origen, catalogoMap, ORIGENES_ESTANDAR);
    }

    public static String formatearOrigen(String origen) {
        return formatearOrigen(origen, null);
    }

    /**
     * Formatea un valor de categoría de incidente resolviendo catálogos dinámicos o valores estándar.
     */
    public static String formatearCategoria(String categoria, Map<String, String> catalogoMap) {
        return resolverValor(categoria, catalogoMap, CATEGORIAS_ESTANDAR);
    }

    public static String formatearCategoria(String categoria) {
        return formatearCategoria(categoria, null);
    }

    /**
     * Formatea un valor de prioridad resolviendo catálogos dinámicos o valores estándar.
     */
    public static String formatearPrioridad(String prioridad, Map<String, String> catalogoMap) {
        return resolverValor(prioridad, catalogoMap, PRIORIDADES_ESTANDAR);
    }

    public static String formatearPrioridad(String prioridad) {
        return formatearPrioridad(prioridad, null);
    }

    /**
     * Formatea un valor de estado resolviendo catálogos dinámicos o valores estándar.
     */
    public static String formatearEstado(String estado, Map<String, String> catalogoMap) {
        return resolverValor(estado, catalogoMap, ESTADOS_ESTANDAR);
    }

    public static String formatearEstado(String estado) {
        return formatearEstado(estado, null);
    }

    /**
     * Formatea un valor de tipo de incidente resolviendo catálogos dinámicos o valores estándar.
     */
    public static String formatearTipo(String tipo, Map<String, String> catalogoMap) {
        return resolverValor(tipo, catalogoMap, TIPOS_ESTANDAR);
    }

    public static String formatearTipo(String tipo) {
        return formatearTipo(tipo, null);
    }

    /**
     * Resuelve un valor contra el mapa dinámico, el mapa estándar o el algoritmo de capitalización.
     */
    private static String resolverValor(String valor, Map<String, String> catalogoMap, Map<String, String> estandarMap) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        String clave = valor.trim().toUpperCase();

        // 1. Intentar resolver desde catálogos persistidos en BD
        if (catalogoMap != null && catalogoMap.containsKey(clave)) {
            String etiqueta = catalogoMap.get(clave);
            if (etiqueta != null && !etiqueta.isBlank()) {
                return etiqueta;
            }
        }

        // 2. Intentar resolver desde diccionario estándar en memoria
        if (estandarMap != null && estandarMap.containsKey(clave)) {
            return estandarMap.get(clave);
        }

        // 3. Fallback inteligente a Title Case
        return capitalizarTexto(valor);
    }

    /**
     * Capitaliza un texto en mayúsculas o formato SNAKE_CASE a Title Case legible.
     * Ejemplo: "CAMARA_SEGURIDAD" -> "Cámara de Seguridad" (si no estaba en catálogo)
     *          "PATRULLAJE_PREVENTIVO" -> "Patrullaje Preventivo"
     */
    public static String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }

        String str = texto.trim();
        if (SIGLAS.contains(str.toUpperCase())) {
            return str.toUpperCase();
        }

        // Si ya contiene minúsculas y no tiene guiones bajos, mantener tal cual
        boolean tieneGuionesBajos = str.contains("_");
        boolean esTodoMayusculas = str.equals(str.toUpperCase()) && str.length() > 1;

        if (!tieneGuionesBajos && !esTodoMayusculas) {
            return str;
        }

        String[] palabras = str.toLowerCase().replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];
            if (palabra.isEmpty()) continue;

            if (sb.length() > 0) {
                sb.append(' ');
            }

            if (SIGLAS.contains(palabra.toUpperCase())) {
                sb.append(palabra.toUpperCase());
            } else if (i > 0 && CONECTORES.contains(palabra)) {
                sb.append(palabra);
            } else {
                sb.append(Character.toUpperCase(palabra.charAt(0)));
                if (palabra.length() > 1) {
                    sb.append(palabra.substring(1));
                }
            }
        }

        return sb.toString();
    }
}
