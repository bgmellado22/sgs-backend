package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

// order "001" define que este es el primer script a ejecutar
@ChangeUnit(id = "init-catalogos", order = "001", author = "conectatech")
public class V1__InitCatalogos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // Aseguramos que la colección exista
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        // Insertamos un catálogo de prueba por defecto de forma nativa
        Document categoria = new Document("tipo", "CATEGORIA")
                .append("valor", "INCIVILIDAD")
                .append("etiqueta", "Incivilidad (Ruidos, basura)")
                .append("activo", true);

        mongoTemplate.insert(categoria, "catalogos");
    }

    @RollbackExecution
    public void rollback() {
        // Lógica de reversión obligatoria por el framework (se deja vacía o se elimina
        // el dato)
    }
}