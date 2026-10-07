package com.conectatech.sgs_backend.service;

import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO;
import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO.CategoriaDistribucion;
import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO.FranjaHoraria;
import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO.Hotspot;
import com.conectatech.sgs_backend.dto.DashboardEstadisticasDTO.TendenciaDiaria;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.Sector;
import com.conectatech.sgs_backend.repository.CatalogoRepository;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estadísticas analíticas para el Dashboard ejecutivo.
 * Las consultas usan los nombres de campo reales de la colección "incidentes"
 * (ej. fecha_creacion), ya que se ejecutan como pipelines sin mapeo de entidad.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String ZONA_HORARIA = "America/Santiago";
    private static final ZoneId ZONA = ZoneId.of(ZONA_HORARIA);
    private static final int DIAS_VENTANA = 7;

    private static final String[] DIAS_SEMANA = { "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom" };

    private final MongoTemplate mongoTemplate;
    private final CatalogoRepository catalogoRepository;
    private final SectorService sectorService;

    public DashboardEstadisticasDTO obtenerEstadisticas() {
        // Ventana actual: hoy + 6 días previos. Ventana anterior: los 7 días antes de esa.
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate inicioSemana = hoy.minusDays(DIAS_VENTANA - 1);
        Date desde = toDate(inicioSemana);
        Date desdeAnterior = toDate(inicioSemana.minusDays(DIAS_VENTANA));

        Document resultado = ejecutarAgregacion(desde, desdeAnterior);

        // ── Estados y tasa de resolución ──
        Map<String, Long> porEstado = aMapa(resultado, "estados");
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();
        long pendientes = porEstado.getOrDefault("Pendiente", 0L);
        long enProceso = porEstado.getOrDefault("En proceso", 0L);
        long cerrados = porEstado.getOrDefault("Resuelto", 0L) + porEstado.getOrDefault("Cerrado", 0L);

        // ── Variación semanal ──
        Map<String, Long> porSemana = aMapa(resultado, "semanas");
        long semanaActual = porSemana.getOrDefault("ACTUAL", 0L);
        long semanaAnterior = porSemana.getOrDefault("ANTERIOR", 0L);
        Double variacion = semanaAnterior == 0
                ? null
                : redondear((double) (semanaActual - semanaAnterior) / semanaAnterior * 100);

        List<FranjaHoraria> franjas = construirFranjas(aMapa(resultado, "horas"));
        FranjaHoraria franjaCritica = franjas.stream()
                .filter(f -> f.getCantidad() > 0)
                .max(Comparator.comparingLong(FranjaHoraria::getCantidad))
                .orElse(null);

        return DashboardEstadisticasDTO.builder()
                .totalIncidentes(total)
                .incidentesSemanaActual(semanaActual)
                .incidentesSemanaAnterior(semanaAnterior)
                .variacionPorcentual(variacion)
                .pendientes(pendientes)
                .enProceso(enProceso)
                .cerrados(cerrados)
                .tasaResolucion(total == 0 ? 0.0 : redondear((double) cerrados / total * 100))
                .tendenciaSemanal(construirTendencia(aMapa(resultado, "dias"), inicioSemana))
                .distribucionHoraria(franjas)
                .franjaCritica(franjaCritica)
                .hotspots(construirHotspots(desde))
                .distribucionCategorias(construirCategorias(aMapa(resultado, "categorias")))
                .build();
    }

    /**
     * Una sola consulta con $facet sobre los incidentes activos.
     */
    private Document ejecutarAgregacion(Date desde, Date desdeAnterior) {
        Document fechaHoraLocal = new Document("date", "$fecha_creacion").append("timezone", ZONA_HORARIA);
        Document enSemanaActual = new Document("$match", new Document("fecha_creacion", new Document("$gte", desde)));

        Document facet = new Document()
                .append("estados", List.of(agruparPor("$estado")))
                .append("categorias", List.of(agruparPor("$categoria")))
                .append("semanas", List.of(
                        new Document("$match", new Document("fecha_creacion", new Document("$gte", desdeAnterior))),
                        agruparPor(new Document("$cond", Arrays.asList(
                                new Document("$gte", Arrays.asList("$fecha_creacion", desde)),
                                "ACTUAL",
                                "ANTERIOR")))))
                .append("dias", List.of(
                        enSemanaActual,
                        agruparPor(new Document("$dateToString",
                                new Document(fechaHoraLocal).append("format", "%Y-%m-%d")))))
                .append("horas", List.of(
                        enSemanaActual,
                        agruparPor(new Document("$hour", fechaHoraLocal))));

        List<Document> pipeline = List.of(
                new Document("$match", new Document("activo", true)),
                new Document("$facet", facet));

        Document resultado = mongoTemplate.getCollection("incidentes").aggregate(pipeline).first();
        return resultado != null ? resultado : new Document();
    }

    private Document agruparPor(Object expresion) {
        return new Document("$group", new Document("_id", expresion)
                .append("cantidad", new Document("$sum", 1)));
    }

    /**
     * Convierte el resultado de una faceta [{_id, cantidad}] en un mapa clave -> cantidad.
     * Las claves nulas (ej. incidentes sin categoría) se agrupan como "null".
     */
    private Map<String, Long> aMapa(Document resultado, String faceta) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Document fila : resultado.getList(faceta, Document.class, List.of())) {
            Object id = fila.get("_id");
            long cantidad = ((Number) fila.get("cantidad")).longValue();
            mapa.merge(String.valueOf(id), cantidad, Long::sum);
        }
        return mapa;
    }

    /**
     * Siete puntos consecutivos, rellenando con 0 los días sin incidentes.
     */
    private List<TendenciaDiaria> construirTendencia(Map<String, Long> porDia, LocalDate inicioSemana) {
        List<TendenciaDiaria> tendencia = new ArrayList<>();
        for (int i = 0; i < DIAS_VENTANA; i++) {
            LocalDate fecha = inicioSemana.plusDays(i);
            String dia = DIAS_SEMANA[fecha.getDayOfWeek().getValue() - 1];
            tendencia.add(new TendenciaDiaria(fecha, dia, porDia.getOrDefault(fecha.toString(), 0L)));
        }
        return tendencia;
    }

    /**
     * Agrupa las 24 horas en cuatro franjas de 6 horas.
     */
    private List<FranjaHoraria> construirFranjas(Map<String, Long> porHora) {
        long[] cantidades = new long[4];
        porHora.forEach((hora, cantidad) -> {
            if (!"null".equals(hora)) {
                cantidades[Integer.parseInt(hora) / 6] += cantidad;
            }
        });

        return List.of(
                new FranjaHoraria("MADRUGADA", "Madrugada", "00:00 - 06:00", cantidades[0]),
                new FranjaHoraria("MANANA", "Mañana", "06:00 - 12:00", cantidades[1]),
                new FranjaHoraria("TARDE", "Tarde", "12:00 - 18:00", cantidades[2]),
                new FranjaHoraria("NOCHE", "Noche", "18:00 - 00:00", cantidades[3]));
    }

    /**
     * Ranking de sectores de la última semana. La asignación al sector más cercano
     * se resuelve en memoria, ya que los sectores son puntos y no polígonos.
     */
    private List<Hotspot> construirHotspots(Date desde) {
        List<Sector> sectores = sectorService.obtenerActivos();
        Map<String, Long> porSector = new HashMap<>();

        mongoTemplate.getCollection("incidentes")
                .find(Filters.and(Filters.eq("activo", true), Filters.gte("fecha_creacion", desde)))
                .projection(Projections.include("location"))
                .forEach(doc -> {
                    Double longitud = null;
                    Double latitud = null;
                    Document location = doc.get("location", Document.class);
                    if (location != null) {
                        List<Double> coordenadas = location.getList("coordinates", Double.class);
                        if (coordenadas != null && coordenadas.size() == 2) {
                            longitud = coordenadas.get(0);
                            latitud = coordenadas.get(1);
                        }
                    }
                    String sector = sectorService.resolverSector(longitud, latitud, sectores);
                    porSector.merge(sector, 1L, Long::sum);
                });

        return porSector.entrySet().stream()
                .map(e -> new Hotspot(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Hotspot::getCantidad).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Distribución por categoría, con etiqueta y color tomados del catálogo
     * para que el gráfico y la tabla usen la misma paleta.
     */
    private List<CategoriaDistribucion> construirCategorias(Map<String, Long> porCategoria) {
        Map<String, Catalogo> catalogo = catalogoRepository.findByTipoAndActivoTrue("CATEGORIA").stream()
                .collect(Collectors.toMap(Catalogo::getValor, Function.identity(), (a, b) -> a));

        return porCategoria.entrySet().stream()
                .map(e -> {
                    String valor = "null".equals(e.getKey()) ? null : e.getKey();
                    Catalogo item = valor != null ? catalogo.get(valor) : null;
                    String etiqueta = item != null ? item.getEtiqueta() : (valor != null ? valor : "Sin categoría");
                    String color = item != null ? item.getColorHex() : null;
                    return new CategoriaDistribucion(valor, etiqueta, color, e.getValue());
                })
                .sorted(Comparator.comparingLong(CategoriaDistribucion::getCantidad).reversed())
                .collect(Collectors.toList());
    }

    private Date toDate(LocalDate fecha) {
        ZonedDateTime inicioDia = fecha.atStartOfDay(ZONA);
        return Date.from(inicioDia.toInstant());
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
