package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "renombrar-campos-sla-a-tiempo-respuesta", order = "008", author = "conectatech")
public class V8__RenombrarCamposSlaATiempoRespuesta {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("incidentes")) {
            mongoTemplate.getCollection("incidentes").updateMany(
                    new Document(),
                    new Document("$rename", new Document("sla_minutos_objetivo", "tiempo_respuesta_minutos_objetivo")
                            .append("fecha_vencimiento_sla", "fecha_vencimiento_tiempo_respuesta"))
            );
        }

        if (mongoTemplate.collectionExists("parametros_sistema")) {
            mongoTemplate.getCollection("parametros_sistema").find().forEach(doc -> {
                String clave = doc.getString("clave");
                if (clave != null && clave.startsWith("SLA_")) {
                    String nuevaClave = clave.replaceFirst("^SLA_", "TIEMPO_RESPUESTA_");
                    mongoTemplate.getCollection("parametros_sistema").updateOne(
                            new Document("_id", doc.get("_id")),
                            new Document("$set", new Document("clave", nuevaClave))
                    );
                }
            });
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("incidentes")) {
            mongoTemplate.getCollection("incidentes").updateMany(
                    new Document(),
                    new Document("$rename", new Document("tiempo_respuesta_minutos_objetivo", "sla_minutos_objetivo")
                            .append("fecha_vencimiento_tiempo_respuesta", "fecha_vencimiento_sla"))
            );
        }

        if (mongoTemplate.collectionExists("parametros_sistema")) {
            mongoTemplate.getCollection("parametros_sistema").find().forEach(doc -> {
                String clave = doc.getString("clave");
                if (clave != null && clave.startsWith("TIEMPO_RESPUESTA_")) {
                    String anteriorClave = clave.replaceFirst("^TIEMPO_RESPUESTA_", "SLA_");
                    mongoTemplate.getCollection("parametros_sistema").updateOne(
                            new Document("_id", doc.get("_id")),
                            new Document("$set", new Document("clave", anteriorClave))
                    );
                }
            });
        }
    }
}
