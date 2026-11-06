package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Ajusta el validador $jsonSchema de la colección 'bitacora_procedimientos'
 * para permitir que 'incidenteId' sea opcional (o nulo) en eventos de auditoría
 * del sistema, configuración, catálogos o usuarios que no provienen de un incidente específico.
 */
@ChangeUnit(id = "ajustar-validador-bitacora", order = "011", author = "conectatech")
public class V11__AjustarValidadorBitacora {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("bitacora_procedimientos")) {
            return;
        }

        try {
            Document listCollectionsCmd = new Document("listCollections", 1)
                    .append("filter", new Document("name", "bitacora_procedimientos"));
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
                        if (jsonSchema != null) {
                            // 1. Remover incidenteId de la lista de campos obligatorios
                            if (jsonSchema.containsKey("required")) {
                                List<String> requiredList = new ArrayList<>(jsonSchema.getList("required", String.class));
                                requiredList.remove("incidenteId");
                                jsonSchema.put("required", requiredList);
                            }

                            // 2. Permitir que incidenteId sea tipo string o null
                            if (jsonSchema.containsKey("properties")) {
                                Document properties = jsonSchema.get("properties", Document.class);
                                Document incidenteIdProp = properties.get("incidenteId", Document.class);
                                if (incidenteIdProp != null) {
                                    incidenteIdProp.put("bsonType", List.of("string", "null"));
                                    incidenteIdProp.put("description", "Referencia lógica al _id del incidente auditado (opcional si es evento de sistema/catálogo)");
                                }
                            }

                            Document collModCmd = new Document("collMod", "bitacora_procedimientos")
                                    .append("validator", validator)
                                    .append("validationLevel", options.getOrDefault("validationLevel", "moderate"))
                                    .append("validationAction", options.getOrDefault("validationAction", "error"));
                            mongoTemplate.getDb().runCommand(collModCmd);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Si el entorno no tiene permisos de admin para collMod, continuar sin bloquear
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // En rollback se mantiene el validador flexible para no romper registros existentes de sistema
    }
}

