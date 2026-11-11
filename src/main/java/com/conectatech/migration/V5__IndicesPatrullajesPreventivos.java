package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Creación de colecciones e índices para el módulo de patrullajes preventivos georreferenciados.
 */
@ChangeUnit(id = "indices-patrullajes-preventivos", order = "005", author = "conectatech")
public class V5__IndicesPatrullajesPreventivos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("patrullajes")) {
            mongoTemplate.createCollection("patrullajes");
        }

        // Índice espacial 2dsphere sobre ubicación
        mongoTemplate.getCollection("patrullajes").createIndex(
                new Document("location", "2dsphere")
        );

        // Índice compuesto para filtros tácticos de servicios activos por estado y fecha
        mongoTemplate.getCollection("patrullajes").createIndex(
                new Document("activo", 1)
                        .append("estado", 1)
                        .append("fecha_creacion", -1)
        );

        // Índice compuesto para validación de disponibilidad y asignaciones por inspector
        mongoTemplate.getCollection("patrullajes").createIndex(
                new Document("inspector_id", 1)
                        .append("estado", 1)
        );
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("patrullajes")) {
            try {
                mongoTemplate.getCollection("patrullajes").dropIndex(
                        new Document("location", "2dsphere")
                );
            } catch (Exception ignored) {}

            try {
                mongoTemplate.getCollection("patrullajes").dropIndex(
                        new Document("activo", 1)
                                .append("estado", 1)
                                .append("fecha_creacion", -1)
                );
            } catch (Exception ignored) {}

            try {
                mongoTemplate.getCollection("patrullajes").dropIndex(
                        new Document("inspector_id", 1)
                                .append("estado", 1)
                );
            } catch (Exception ignored) {}
        }
    }
}

