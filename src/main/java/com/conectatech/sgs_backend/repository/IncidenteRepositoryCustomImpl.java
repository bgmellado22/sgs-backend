package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.dto.ReporteKpiDTO;
import com.conectatech.sgs_backend.model.Catalogo;
import com.conectatech.sgs_backend.model.Incidente;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.bson.Document;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class IncidenteRepositoryCustomImpl implements IncidenteRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    // Meta de respaldo (48h) para incidentes creados antes de configurar los tiempos de respuesta por prioridad
    private static final long TIEMPO_RESPUESTA_POR_DEFECTO_MINUTOS = 2880;

    private Query construirQueryFiltros(
            String textoBusqueda, String categoria, String tipo,
            String estado, String prioridad, LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        Query query = new Query();
        List<Criteria> criterios = new ArrayList<>();

        // Filtros base: solo mostrar incidentes activos
        criterios.add(Criteria.where("activo").is(true));

        // Búsqueda por texto (descripción, código correlativo o datos del denunciante)
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            Criteria textoCriteria = new Criteria().orOperator(
                    Criteria.where("descripcion").regex(textoBusqueda, "i"),
                    Criteria.where("codigoCorrelativo").regex(textoBusqueda, "i"),
                    Criteria.where("denunciante.nombres").regex(textoBusqueda, "i"),
                    Criteria.where("denunciante.primer_apellido").regex(textoBusqueda, "i"),
                    Criteria.where("denunciante.run").regex(textoBusqueda, "i"),
                    Criteria.where("denunciante.telefono").regex(textoBusqueda, "i"));
            criterios.add(textoCriteria);
        }

        // Filtros exactos opcionales (catálogos y estados)
        if (categoria != null && !categoria.isBlank()) {
            criterios.add(Criteria.where("categoria").is(categoria));
        }
        if (tipo != null && !tipo.isBlank()) {
            criterios.add(Criteria.where("tipo").is(tipo));
        }
        if (estado != null && !estado.isBlank()) {
            criterios.add(Criteria.where("estado").is(estado));
        }
        if (prioridad != null && !prioridad.isBlank()) {
            criterios.add(Criteria.where("prioridad").is(prioridad));
        }

        // Filtro por rango de fechas de creación
        if (fechaInicio != null && fechaFin != null) {
            criterios.add(Criteria.where("fechaCreacion").gte(fechaInicio).lte(fechaFin));
        } else if (fechaInicio != null) {
            criterios.add(Criteria.where("fechaCreacion").gte(fechaInicio));
        } else if (fechaFin != null) {
            criterios.add(Criteria.where("fechaCreacion").lte(fechaFin));
        }

        // Ensamblar
        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        return query;
    }

    @Override
    public List<Incidente> buscarConFiltrosAvanzados(
            String textoBusqueda, String categoria, String tipo,
            String estado, String prioridad, LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        Query query = construirQueryFiltros(textoBusqueda, categoria, tipo, estado, prioridad, fechaInicio, fechaFin);
        return mongoTemplate.find(query, Incidente.class);
    }

    @Override
    public Page<Incidente> buscarConFiltrosAvanzadosPaginado(
            String textoBusqueda, String categoria, String tipo,
            String estado, String prioridad, LocalDateTime fechaInicio,
            LocalDateTime fechaFin, Pageable pageable) {
        Query query = construirQueryFiltros(textoBusqueda, categoria, tipo, estado, prioridad, fechaInicio, fechaFin);
        long total = mongoTemplate.count(query, Incidente.class);
        query.with(pageable);
        List<Incidente> resultados = mongoTemplate.find(query, Incidente.class);
        return new PageImpl<>(resultados, pageable, total);
    }

    @Override
    public ReporteKpiDTO calcularKpis(String categoria, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // La agregación se ejecuta sobre "incidentes" sin mapeo de entidad,
        // por lo que se usan los nombres de campo reales (fecha_creacion)
        Criteria criteria = Criteria.where("activo").is(true);

        if (categoria != null && !categoria.isBlank()) {
            criteria = criteria.and("categoria").is(categoria);
        }

        // Filtro dinámico de fechas
        if (fechaInicio != null && fechaFin != null) {
            criteria = criteria.and("fecha_creacion").gte(fechaInicio).lte(fechaFin);
        } else if (fechaInicio != null) {
            criteria = criteria.and("fecha_creacion").gte(fechaInicio);
        } else if (fechaFin != null) {
            criteria = criteria.and("fecha_creacion").lte(fechaFin);
        }

        List<String> prioridadesCriticas = obtenerPrioridadesCriticas();

        org.springframework.data.mongodb.core.aggregation.AggregationExpression sumExpr = context -> 
                new Document("$cond", new Document("if", 
                        new Document("$in", java.util.Arrays.asList("$estado", java.util.Arrays.asList("Resuelto", "Cerrado"))))
                .append("then", 1)
                .append("else", 0));

        org.springframework.data.mongodb.core.aggregation.AggregationExpression sumTiempoRespuestaExpr = context -> 
                new Document("$cond", new Document("if", 
                        new Document("$and", java.util.Arrays.asList(
                                new Document("$in", java.util.Arrays.asList("$estado", java.util.Arrays.asList("Resuelto", "Cerrado"))),
                                new Document("$ne", java.util.Arrays.asList("$tiempo_resolucion_minutos", null)),
                                // Meta propia del incidente (según su prioridad); 48h para incidentes sin tiempo de respuesta asignado
                                new Document("$lte", java.util.Arrays.asList(
                                        "$tiempo_resolucion_minutos",
                                        new Document("$ifNull", java.util.Arrays.asList(
                                                new Document("$ifNull", java.util.Arrays.asList("$tiempo_respuesta_minutos_objetivo", "$sla_minutos_objetivo")),
                                                TIEMPO_RESPUESTA_POR_DEFECTO_MINUTOS))))
                        ))
                )
                .append("then", 1)
                .append("else", 0));

        org.springframework.data.mongodb.core.aggregation.AggregationExpression sumCritExpr = context -> 
                new Document("$cond", new Document("if", 
                        new Document("$in", java.util.Arrays.asList(
                                new Document("$toUpper", new Document("$ifNull", java.util.Arrays.asList("$prioridad", ""))),
                                prioridadesCriticas)))
                .append("then", 1)
                .append("else", 0));

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.facet(
                        Aggregation.group()
                                .count().as("volumenOperativoTotal")
                                .sum(sumExpr).as("casosResueltos")
                                .sum(sumTiempoRespuestaExpr).as("casosTiempoRespuesta")
                                .sum(sumCritExpr).as("casosCriticos")
                ).as("kpis")
                .and(
                        Aggregation.group("categoria").count().as("cantidad"),
                        Aggregation.sort(Sort.Direction.DESC, "cantidad")
                ).as("categorias")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "incidentes", Document.class);
        Document resultDoc = results.getUniqueMappedResult();

        if (resultDoc == null) {
            return new ReporteKpiDTO(0L, 0.0, 0.0, 0.0, new ArrayList<>());
        }

        List<Document> kpisList = resultDoc.getList("kpis", Document.class);
        Document kpiDoc = (kpisList != null && !kpisList.isEmpty()) ? kpisList.get(0) : null;

        long total = kpiDoc != null ? ((Number) kpiDoc.getOrDefault("volumenOperativoTotal", 0)).longValue() : 0L;
        long resueltos = kpiDoc != null ? ((Number) kpiDoc.getOrDefault("casosResueltos", 0)).longValue() : 0L;
        long casosTiempoRespuesta = kpiDoc != null ? ((Number) kpiDoc.getOrDefault("casosTiempoRespuesta", 0)).longValue() : 0L;
        long casosCriticos = kpiDoc != null ? ((Number) kpiDoc.getOrDefault("casosCriticos", 0)).longValue() : 0L;

        double tasa = (total == 0) ? 0.0 : Math.round(((double) resueltos / total) * 100.0 * 10.0) / 10.0;
        double cumplimientoTiempoRespuesta = (resueltos == 0) ? 0.0 : Math.round(((double) casosTiempoRespuesta / resueltos) * 100.0 * 10.0) / 10.0;
        double indiceCriticidad = (total == 0) ? 0.0 : Math.round(((double) casosCriticos / total) * 100.0 * 10.0) / 10.0;

        // Mapear categorías con etiquetas y colores del catálogo
        Map<String, Catalogo> catalogoMap = obtenerCatalogoCategorias();
        List<Document> categoriasDocs = resultDoc.getList("categorias", Document.class);
        List<ReporteKpiDTO.CategoriaDistribucion> distribucionCategorias = new ArrayList<>();

        if (categoriasDocs != null) {
            for (Document catDoc : categoriasDocs) {
                Object idObj = catDoc.get("_id");
                String valor = (idObj == null || "null".equals(idObj)) ? null : String.valueOf(idObj);
                long cantidad = ((Number) catDoc.getOrDefault("cantidad", 0)).longValue();
                Catalogo item = valor != null ? catalogoMap.get(valor) : null;
                String etiqueta = item != null ? item.getEtiqueta() : (valor != null ? valor : "Sin categoría");
                String colorHex = item != null ? item.getColorHex() : null;
                Integer nivelOrden = item != null ? item.getNivelOrden() : 0;
                distribucionCategorias.add(new ReporteKpiDTO.CategoriaDistribucion(valor, etiqueta, colorHex, cantidad, nivelOrden));
            }
        }

        return new ReporteKpiDTO(total, tasa, cumplimientoTiempoRespuesta, indiceCriticidad, distribucionCategorias);
    }

    /**
     * Catálogo activo de categorías para enriquecer las etiquetas y colores de la distribución.
     */
    private Map<String, Catalogo> obtenerCatalogoCategorias() {
        List<Catalogo> categorias = mongoTemplate.find(
                Query.query(Criteria.where("tipo").is("CATEGORIA").and("activo").is(true)),
                Catalogo.class);
        return categorias.stream()
                .filter(c -> c.getValor() != null)
                .collect(Collectors.toMap(Catalogo::getValor, Function.identity(), (a, b) -> a));
    }

    /**
     * Prioridades críticas: ALTA y cualquier prioridad del catálogo con un
     * nivelOrden igual o superior (ej. una CRITICA creada por el administrador).
     * Valores en mayúsculas para comparar sin importar cómo quedaron guardados.
     */
    private List<String> obtenerPrioridadesCriticas() {
        List<Catalogo> prioridades = mongoTemplate.find(
                Query.query(Criteria.where("tipo").is("PRIORIDAD").and("activo").is(true)),
                Catalogo.class);

        Integer nivelAlta = prioridades.stream()
                .filter(p -> "ALTA".equalsIgnoreCase(p.getValor()) && p.getNivelOrden() != null)
                .map(Catalogo::getNivelOrden)
                .findFirst()
                .orElse(null);

        List<String> criticas = new ArrayList<>();
        criticas.add("ALTA");
        if (nivelAlta != null) {
            prioridades.stream()
                    .filter(p -> p.getNivelOrden() != null && p.getNivelOrden() >= nivelAlta)
                    .map(p -> p.getValor().toUpperCase())
                    .filter(v -> !criticas.contains(v))
                    .forEach(criticas::add);
        }
        return criticas;
    }
}
