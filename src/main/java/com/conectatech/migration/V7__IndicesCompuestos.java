package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "indices-compuestos", order = "007", author = "conectatech")
public class V7__IndicesCompuestos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("incidentes")) {
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
        }

        if (mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.getCollection("catalogos").createIndex(
                    new Document("tipo", 1)
                            .append("activo", 1)
            );
        }
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
