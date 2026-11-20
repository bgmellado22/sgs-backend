package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Migración V9: Creación de índices compuestos en la colección 'notificaciones'
 * para optimizar la paginación de scroll infinito y el recuento de no leídas.
 */
@ChangeUnit(id = "indices-notificaciones", order = "009", author = "conectatech")
public class V9__IndicesNotificaciones {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("notificaciones")) {
            mongoTemplate.createCollection("notificaciones");
        }

        // 1. Índice compuesto para paginación ordenada por fecha descendente
        mongoTemplate.getCollection("notificaciones").createIndex(
                new Document("usuarioDestinoId", 1)
                        .append("fechaCreacion", -1)
        );

        // 2. Índice compuesto para conteo y filtrado de notificaciones por estado
        mongoTemplate.getCollection("notificaciones").createIndex(
                new Document("usuarioDestinoId", 1)
                        .append("estado", 1)
                        .append("fechaCreacion", -1)
        );
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("notificaciones")) {
            try {
                mongoTemplate.getCollection("notificaciones").dropIndex(
                        new Document("usuarioDestinoId", 1)
                                .append("fechaCreacion", -1)
                );
            } catch (Exception ignored) {}

            try {
                mongoTemplate.getCollection("notificaciones").dropIndex(
                        new Document("usuarioDestinoId", 1)
                                .append("estado", 1)
                                .append("fechaCreacion", -1)
                );
            } catch (Exception ignored) {}
        }
    }
}
