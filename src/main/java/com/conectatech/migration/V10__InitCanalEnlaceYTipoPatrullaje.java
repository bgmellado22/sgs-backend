package com.conectatech.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Inicialización de catálogos para Canal de Enlace (Derivación a Carabineros)
 * y Tipo de Patrullaje (Patrullaje Preventivo).
 * Actualiza el $jsonSchema de validación de MongoDB en la colección 'catalogos' para admitir TIPO_PATRULLAJE.
 */
@ChangeUnit(id = "init-canal-enlace-y-tipo-patrullaje", order = "010", author = "conectatech")
public class V10__InitCanalEnlaceYTipoPatrullaje {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        // 1. Actualizar el validador $jsonSchema de la colección para aceptar TIPO_PATRULLAJE
        actualizarValidadorSiAplica(mongoTemplate);

        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");

        // 2. Canales de Enlace para Derivación a Carabineros
        asegurarCatalogo(catalogos, "CANAL_ENLACE", "RADIAL_CENCO", "Frecuencia Radial CENCO", 1);
        asegurarCatalogo(catalogos, "CANAL_ENLACE", "TELEFONO_CUADRANTE", "Teléfono Directo Cuadrante", 2);
        asegurarCatalogo(catalogos, "CANAL_ENLACE", "CORREO_INSTITUCIONAL", "Correo Institucional", 3);
        asegurarCatalogo(catalogos, "CANAL_ENLACE", "OFICIO_CONDUCTOR", "Oficio Conductor", 4);

        // 3. Tipos de Patrullaje Preventivo
        asegurarCatalogo(catalogos, "TIPO_PATRULLAJE", "PREVENTIVO", "Patrullaje Preventivo", 1);
        asegurarCatalogo(catalogos, "TIPO_PATRULLAJE", "BORDE_COSTERO", "Patrullaje Borde Costero", 2);
        asegurarCatalogo(catalogos, "TIPO_PATRULLAJE", "ESCOLAR", "Patrullaje Escolar", 3);
        asegurarCatalogo(catalogos, "TIPO_PATRULLAJE", "FOCALIZADO", "Patrullaje Focalizado", 4);
        asegurarCatalogo(catalogos, "TIPO_PATRULLAJE", "NOCTURNO", "Patrullaje Nocturno", 5);
    }

    private void actualizarValidadorSiAplica(MongoTemplate mongoTemplate) {
        try {
            Document listCollectionsCmd = new Document("listCollections", 1)
                    .append("filter", new Document("name", "catalogos"));
            Document result = mongoTemplate.getDb().runCommand(listCollectionsCmd);
            Document cursor = result.get("cursor", Document.class);
            if (cursor != null) {
                List<Document> firstBatch = cursor.getList("firstBatch", Document.class);
                if (firstBatch != null && !firstBatch.isEmpty()) {
                    Document collInfo = firstBatch.get(0);
                    Document options = collInfo.get("options", Document.class);
                    if (options != null && options.containsKey("validator")) {
                        Document validator = options.get("validator", Document.class);
                        Document jsonSchema = validator.get("$jsonSchema", Document.class);
                        if (jsonSchema != null && jsonSchema.containsKey("properties")) {
                            Document properties = jsonSchema.get("properties", Document.class);
                            Document tipoProp = properties.get("tipo", Document.class);
                            if (tipoProp != null && tipoProp.containsKey("enum")) {
                                List<String> enumList = new ArrayList<>(tipoProp.getList("enum", String.class));
                                boolean modificado = false;
                                if (!enumList.contains("CANAL_ENLACE")) {
                                    enumList.add("CANAL_ENLACE");
                                    modificado = true;
                                }
                                if (!enumList.contains("TIPO_PATRULLAJE")) {
                                    enumList.add("TIPO_PATRULLAJE");
                                    modificado = true;
                                }
                                if (modificado) {
                                    tipoProp.put("enum", enumList);
                                    Document collModCmd = new Document("collMod", "catalogos")
                                            .append("validator", validator)
                                            .append("validationLevel", options.getOrDefault("validationLevel", "moderate"))
                                            .append("validationAction", options.getOrDefault("validationAction", "error"));
                                    mongoTemplate.getDb().runCommand(collModCmd);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Si el motor no admite collMod o no tiene permisos de admin, no bloquear la migración
        }
    }

    private void asegurarCatalogo(MongoCollection<Document> catalogos,
                                  String tipo, String valor, String etiqueta,
                                  int nivelOrden) {
        Bson filtro = Filters.and(Filters.eq("tipo", tipo), Filters.eq("valor", valor));
        catalogos.updateOne(filtro, Updates.combine(
                Updates.set("etiqueta", etiqueta),
                Updates.set("nivelOrden", nivelOrden),
                Updates.set("activo", true),
                Updates.setOnInsert("_id", new ObjectId()),
                Updates.setOnInsert("tipo", tipo),
                Updates.setOnInsert("valor", valor),
                Updates.setOnInsert("version", 0L)
        ), new UpdateOptions().upsert(true));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("catalogos")) {
            MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");
            catalogos.deleteMany(Filters.in("tipo", List.of("CANAL_ENLACE", "TIPO_PATRULLAJE")));
        }
    }
}
