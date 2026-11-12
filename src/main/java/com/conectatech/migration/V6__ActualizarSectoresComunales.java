package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Actualización oficial de los sectores comunales de El Tabo para
 * georreferenciación y análisis de Zonas Críticas (Hotspots).
 * 
 * Alinea la base de datos con los 6 sectores territoriales oficiales:
 * 1. El Tabo Centro
 * 2. El Tabito
 * 3. Playa Chépica
 * 4. Las Cruces
 * 5. Playas Blancas
 * 6. Laguna El Peral
 */
@ChangeUnit(id = "actualizar-sectores-comunales", order = "006", author = "conectatech")
public class V6__ActualizarSectoresComunales {

    private static final String COLLECTION = "sectores";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists(COLLECTION)) {
            mongoTemplate.createCollection(COLLECTION);
        }

        // 1. Desactivar sectores obsoletos o que no pertenecen a la división territorial oficial
        mongoTemplate.getCollection(COLLECTION).updateMany(
                new Document("nombre", new Document("$in", Arrays.asList("Isla Negra", "San Carlos", "Chépica"))),
                new Document("$set", new Document("activo", false))
        );

        // 2. Insertar o actualizar los 6 sectores comunales oficiales
        List<Document> sectoresOficiales = Arrays.asList(
                sector("6a6000000000000000000002", "El Tabo Centro", -71.6654, -33.4533),
                sector("6a6000000000000000000010", "El Tabito",      -71.6500, -33.4640),
                sector("6a6000000000000000000011", "Playa Chépica",  -71.6550, -33.4650),
                sector("6a6000000000000000000006", "Las Cruces",     -71.6258, -33.4985),
                sector("6a6000000000000000000005", "Playas Blancas", -71.6085, -33.5106),
                sector("6a6000000000000000000012", "Laguna El Peral",-71.6066, -33.5051)
        );

        for (Document doc : sectoresOficiales) {
            mongoTemplate.getCollection(COLLECTION).replaceOne(
                    new Document("_id", doc.getObjectId("_id")),
                    doc,
                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );
        }
    }

    private Document sector(String id, String nombre, double longitud, double latitud) {
        return new Document("_id", new ObjectId(id))
                .append("nombre", nombre)
                .append("centro", new Document("type", "Point")
                        .append("coordinates", Arrays.asList(longitud, latitud)))
                .append("activo", true);
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists(COLLECTION)) {
            return;
        }

        // Eliminar sectores nuevos creados en esta migración
        mongoTemplate.getCollection(COLLECTION).deleteMany(
                new Document("_id", new Document("$in", Arrays.asList(
                        new ObjectId("6a6000000000000000000010"),
                        new ObjectId("6a6000000000000000000011"),
                        new ObjectId("6a6000000000000000000012")
                )))
        );

        // Reactivar sectores originales de V2
        mongoTemplate.getCollection(COLLECTION).updateMany(
                new Document("nombre", new Document("$in", Arrays.asList("Isla Negra", "San Carlos", "Chépica"))),
                new Document("$set", new Document("activo", true))
        );
    }
}

