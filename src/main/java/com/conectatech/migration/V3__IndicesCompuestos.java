package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Creación de índices compuestos para optimizar consultas de alto rendimiento
 * y filtros frecuentes en incidentes y catálogos.
 */
@ChangeUnit(id = "indices-compuestos", order = "003", author = "conectatech")
public class V3__IndicesCompuestos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // 1. Índices para la colección incidentes
        if (!mongoTemplate.collectionExists("incidentes")) {
            mongoTemplate.createCollection("incidentes");
        }

        mongoTemplate.getCollection("incidentes").createIndex(
                new Document("activo", 1)
                        .append("estado", 1)
                        .append("prioridad", 1)
                        .append("fecha_creacion", -1)
        );

        mongoTemplate.getCollection("incidentes").createIndex(
                new Document("activo", 1)
                        .append("categoria", 1)
                        .append("fecha_creacion", -1)
        );

        // 2. Índices para la colección catalogos
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        mongoTemplate.getCollection("catalogos").createIndex(
                new Document("tipo", 1)
                        .append("activo", 1)
        );
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("incidentes")) {
            try {
                mongoTemplate.getCollection("incidentes").dropIndex(
                        new Document("activo", 1)
                                .append("estado", 1)
                                .append("prioridad", 1)
                                .append("fecha_creacion", -1)
                );
            } catch (Exception ignored) {}

            try {
                mongoTemplate.getCollection("incidentes").dropIndex(
                        new Document("activo", 1)
                                .append("categoria", 1)
                                .append("fecha_creacion", -1)
                );
            } catch (Exception ignored) {}
        }

        if (mongoTemplate.collectionExists("catalogos")) {
            try {
                mongoTemplate.getCollection("catalogos").dropIndex(
                        new Document("tipo", 1)
                                .append("activo", 1)
                );
            } catch (Exception ignored) {}
        }
    }
}

