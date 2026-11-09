package com.conectatech.migration;

import com.mongodb.client.model.IndexOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Creación de índice único sobre el RUN de los usuarios para garantizar
 * la integridad referencial del padrón de autorización con ClaveÚnica.
 */
@ChangeUnit(id = "indice-unico-run-usuarios", order = "004", author = "conectatech")
public class V4__IndiceUnicoRunUsuarios {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }

        mongoTemplate.getCollection("usuarios").createIndex(
                new Document("run", 1),
                new IndexOptions().unique(true).sparse(true)
        );
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("usuarios")) {
            try {
                mongoTemplate.getCollection("usuarios").dropIndex(new Document("run", 1));
            } catch (Exception ignored) {
            }
        }
    }
}

