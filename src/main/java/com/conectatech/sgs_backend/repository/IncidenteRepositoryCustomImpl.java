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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class IncidenteRepositoryCustomImpl implements IncidenteRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<Incidente> buscarConFiltrosAvanzados(
            String textoBusqueda, String categoria, String tipo,
            String estado, String prioridad, LocalDateTime fechaInicio,
            LocalDateTime fechaFin) {
        Query query = new Query();
        List<Criteria> criterios = new ArrayList<>();

        // Filtros base: solo mostrar incidentes activos
        criterios.add(Criteria.where("activo").is(true));

        // Búsqueda por texto (descripción o código correlativo)
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            Criteria textoCriteria = new Criteria().orOperator(
                    Criteria.where("descripcion").regex(textoBusqueda, "i"),
                    Criteria.where("codigoCorrelativo").regex(textoBusqueda, "i"));
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

        // Ensamblar y ejecutar
        if (!criterios.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterios.toArray(new Criteria[0])));
        }

        return mongoTemplate.find(query, Incidente.class);

    }

    @Override
    public ReporteKpiDTO calcularKpis(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        // La agregación se ejecuta sobre "incidentes" sin mapeo de entidad,
        // por lo que se usan los nombres de campo reales (fecha_creacion)
        Criteria criteria = Criteria.where("activo").is(true);

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

        org.springframework.data.mongodb.core.aggregation.AggregationExpression sumSlaExpr = context -> 
                new Document("$cond", new Document("if", 
                        new Document("$and", java.util.Arrays.asList(
                                new Document("$in", java.util.Arrays.asList("$estado", java.util.Arrays.asList("Resuelto", "Cerrado"))),
                                new Document("$ne", java.util.Arrays.asList("$tiempo_resolucion_minutos", null)),
                                new Document("$lte", java.util.Arrays.asList("$tiempo_resolucion_minutos", 2880)) // Meta: 48h (2880 mins)
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
                Aggregation.group()
                        .count().as("volumenOperativoTotal")
                        .sum(sumExpr).as("casosResueltos")
                        .sum(sumSlaExpr).as("casosSla")
                        .sum(sumCritExpr).as("casosCriticos"));

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "incidentes", Document.class);
        Document resultDoc = results.getUniqueMappedResult();

        if (resultDoc == null) {
            return new ReporteKpiDTO(0L, 0.0, 0.0, 0.0);
        }

        long total = resultDoc.getInteger("volumenOperativoTotal", 0);
        long resueltos = resultDoc.getInteger("casosResueltos", 0);
        long casosSla = resultDoc.getInteger("casosSla", 0);
        long casosCriticos = resultDoc.getInteger("casosCriticos", 0);

        double tasa = (total == 0) ? 0.0 : Math.round(((double) resueltos / total) * 100.0 * 10.0) / 10.0;
        double cumplimientoSla = (resueltos == 0) ? 0.0 : Math.round(((double) casosSla / resueltos) * 100.0 * 10.0) / 10.0;
        double indiceCriticidad = (total == 0) ? 0.0 : Math.round(((double) casosCriticos / total) * 100.0 * 10.0) / 10.0;

        return new ReporteKpiDTO(total, tasa, cumplimientoSla, indiceCriticidad);
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
