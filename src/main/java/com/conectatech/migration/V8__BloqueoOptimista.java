package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Migración para soporte de Bloqueo Optimista (@Version).
 * Inicializa el campo version en 0L para los documentos existentes
 * en las colecciones incidentes, catalogos y parametros_sistema,
 * previniendo inconsistencias o excepciones por campos nulos al realizar actualizaciones concurrentes.
 */
@ChangeUnit(id = "bloqueo-optimista", order = "008", author = "conectatech")
public class V8__BloqueoOptimista {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        Document filtroSinVersion = new Document("version", new Document("$exists", false));
        Document inicializarVersion = new Document("$set", new Document("version", 0L));

        if (mongoTemplate.collectionExists("incidentes")) {
            mongoTemplate.getCollection("incidentes").updateMany(filtroSinVersion, inicializarVersion);
        }

        if (mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.getCollection("catalogos").updateMany(filtroSinVersion, inicializarVersion);
        }

        if (mongoTemplate.collectionExists("parametros_sistema")) {
            mongoTemplate.getCollection("parametros_sistema").updateMany(filtroSinVersion, inicializarVersion);
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        Document removerVersion = new Document("$unset", new Document("version", ""));
        Document filtroTodos = new Document();

        if (mongoTemplate.collectionExists("incidentes")) {
            try {
                mongoTemplate.getCollection("incidentes").updateMany(filtroTodos, removerVersion);
            } catch (Exception ignored) {}
        }

        if (mongoTemplate.collectionExists("catalogos")) {
            try {
                mongoTemplate.getCollection("catalogos").updateMany(filtroTodos, removerVersion);
            } catch (Exception ignored) {}
        }

        if (mongoTemplate.collectionExists("parametros_sistema")) {
            try {
                mongoTemplate.getCollection("parametros_sistema").updateMany(filtroTodos, removerVersion);
            } catch (Exception ignored) {}
        }
    }
}

