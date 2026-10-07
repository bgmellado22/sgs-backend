package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.List;

// order "003": localidades de la comuna para el ranking territorial (Hotspots)
@ChangeUnit(id = "init-sectores", order = "003", author = "conectatech")
public class V3__InitSectores {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {

        if (!mongoTemplate.collectionExists("sectores")) {
            mongoTemplate.createCollection("sectores");
        }

        if (mongoTemplate.getCollection("sectores").countDocuments() == 0) {

            // Coordenadas aproximadas del centro de cada localidad (de norte a sur por la costa)
            List<Document> sectores = Arrays.asList(
                sector("6a6000000000000000000001", "Isla Negra",     -71.6850, -33.4420),
                sector("6a6000000000000000000002", "El Tabo Centro", -71.6660, -33.4550),
                sector("6a6000000000000000000003", "Chépica",        -71.6580, -33.4650),
                sector("6a6000000000000000000004", "San Carlos",     -71.6480, -33.4760),
                sector("6a6000000000000000000005", "Playas Blancas", -71.6350, -33.4870),
                sector("6a6000000000000000000006", "Las Cruces",     -71.6200, -33.5000)
            );

            mongoTemplate.getCollection("sectores").insertMany(sectores);
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
        if (mongoTemplate.collectionExists("sectores")) {
            mongoTemplate.dropCollection("sectores");
        }
    }
}
