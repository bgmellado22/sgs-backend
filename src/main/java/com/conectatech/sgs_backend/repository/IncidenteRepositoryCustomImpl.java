package com.conectatech.sgs_backend.repository;

import com.conectatech.sgs_backend.model.Incidente;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

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
}
