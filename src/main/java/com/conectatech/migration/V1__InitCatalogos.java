package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.List;

// order "001" define que este es el primer script a ejecutar
@ChangeUnit(id = "init-catalogos", order = "001", author = "conectatech")
public class V1__InitCatalogos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // Verificar si existe la colección
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        List<Document> catalogos = Arrays.asList(
                new Document("_id", new ObjectId("6a5e67a6cc23ff9c6874a218"))
                        .append("tipo", "CATEGORIA")
                        .append("valor", "DELITO")
                        .append("etiqueta", "Delito")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e67a6cc23ff9c6874a219"))
                        .append("tipo", "CATEGORIA")
                        .append("valor", "INCIVILIDAD")
                        .append("etiqueta", "Incivilidad")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e67a6cc23ff9c6874a21a"))
                        .append("tipo", "PRIORIDAD")
                        .append("valor", "ALTA")
                        .append("etiqueta", "Alta")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e67a6cc23ff9c6874a21b"))
                        .append("tipo", "PRIORIDAD")
                        .append("valor", "MEDIA")
                        .append("etiqueta", "Media")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e949acc23ff9c6874a238"))
                        .append("tipo", "ORIGEN")
                        .append("valor", "LLAMADA")
                        .append("etiqueta", "Llamada Telefónica")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e949acc23ff9c6874a239"))
                        .append("tipo", "ORIGEN")
                        .append("valor", "CAMARA_SEGURIDAD")
                        .append("etiqueta", "Cámara de Seguridad")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e949acc23ff9c6874a23a"))
                        .append("tipo", "ORIGEN")
                        .append("valor", "PATRULLA")
                        .append("etiqueta", "Patrulla en Terreno")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a5e949acc23ff9c6874a23b"))
                        .append("tipo", "ORIGEN")
                        .append("valor", "PRESENCIAL")
                        .append("etiqueta", "Atención Presencial")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a872742dba7941a74a7fdea"))
                        .append("tipo", "PRIORIDAD")
                        .append("valor", "BAJA")
                        .append("etiqueta", "Baja")
                        .append("activo", true),
                new Document("_id", new ObjectId("6a8727f5dba7941a74a7fded"))
                        .append("tipo", "CATEGORIA")
                        .append("valor", "SOS")
                        .append("etiqueta", "SOS")
                        .append("activo", true)
        );

        mongoTemplate.insert(catalogos, "catalogos");
    }

    @RollbackExecution
    public void rollback() {
        // Lógica de reversión obligatoria por el framework
    }
}