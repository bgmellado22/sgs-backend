package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;
import java.util.List;

@ChangeUnit(id = "init-tipo-eventos", order = "005", author = "conectatech")
public class V5__InitTipoEventos {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        
        // Verificar si existe la colección, si no, crearla de forma segura
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        // Insertamos los tipos de evento solo si no existen ya
        long countTiposEvento = mongoTemplate.getCollection("catalogos")
                .countDocuments(new Document("tipo", "TIPO_EVENTO"));

        if (countTiposEvento == 0) {
            
            List<Document> catalogos = Arrays.asList(
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "ROBO_HURTO")
                    .append("etiqueta", "Robo / Hurto")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "INCENDIO")
                    .append("etiqueta", "Incendio")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "VIF")
                    .append("etiqueta", "Violencia Intrafamiliar (VIF)")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "CONSUMO_DROGAS")
                    .append("etiqueta", "Consumo de Alcohol/Drogas en Vía Pública")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "RUIDOS_MOLESTOS")
                    .append("etiqueta", "Ruidos Molestos")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "ACCIDENTE_TRANSITO")
                    .append("etiqueta", "Accidente de Tránsito")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "SOSPECHOSO")
                    .append("etiqueta", "Persona Sospechosa")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "COMERCIO_AMBULANTE")
                    .append("etiqueta", "Comercio Ambulante no Autorizado")
                    .append("activo", true),
                    
                new Document("_id", new ObjectId())
                    .append("tipo", "TIPO_EVENTO")
                    .append("valor", "OTRO")
                    .append("etiqueta", "Otro (Especificar en descripción)")
                    .append("activo", true)
            );
            
            // Insertar los documentos 
            mongoTemplate.getCollection("catalogos").insertMany(catalogos);
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.getCollection("catalogos").deleteMany(new Document("tipo", "TIPO_EVENTO"));
        }
    }
}
