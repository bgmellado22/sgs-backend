package com.conectatech.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creación de índice único sobre el RUN de los usuarios para garantizar
 * la integridad referencial del padrón de autorización con ClaveÚnica.
 * Limpia duplicados preexistentes de forma defensiva antes de indexar.
 */
@ChangeUnit(id = "indice-unico-run-usuarios", order = "004", author = "conectatech")
public class V4__IndiceUnicoRunUsuarios {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }

        MongoCollection<Document> usuarios = mongoTemplate.getCollection("usuarios");

        // 1. Limpieza defensiva: si existen RUN duplicados en la base de datos histórica,
        // conservar solo el primer registro y remover los duplicados para evitar error E11000 al indexar.
        List<Document> duplicados = usuarios.aggregate(Arrays.asList(
                new Document("$match", new Document("run", new Document("$ne", null))),
                new Document("$group", new Document("_id", "$run")
                        .append("count", new Document("$sum", 1))
                        .append("ids", new Document("$push", "$_id"))),
                new Document("$match", new Document("count", new Document("$gt", 1)))
        )).into(new ArrayList<>());

        for (Document dup : duplicados) {
            List<Object> ids = dup.getList("ids", Object.class);
            if (ids != null && ids.size() > 1) {
                for (int i = 1; i < ids.size(); i++) {
                    usuarios.deleteOne(new Document("_id", ids.get(i)));
                }
            }
        }

        // 2. Creación del índice único
        usuarios.createIndex(
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
