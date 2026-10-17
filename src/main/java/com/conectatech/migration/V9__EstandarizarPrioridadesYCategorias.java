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

import java.util.List;

/**
 * Estandariza de forma inalterable las Prioridades (Baja, Media, Alta) y las Categorías (SOS, Delito, Incivilidad).
 * Desactiva cualquier otra categoría o prioridad dinámica previa para que estas seis opciones sean el estándar definitivo.
 */
@ChangeUnit(id = "estandarizar-prioridades-y-categorias", order = "009", author = "conectatech")
public class V9__EstandarizarPrioridadesYCategorias {

    // IDs fijos estándar de V1/V2
    private static final ObjectId ID_BAJA        = new ObjectId("6a5e67a6cc23ff9c6874a21d");
    private static final ObjectId ID_MEDIA       = new ObjectId("6a5e67a6cc23ff9c6874a21b");
    private static final ObjectId ID_ALTA        = new ObjectId("6a5e67a6cc23ff9c6874a21a");

    private static final ObjectId ID_SOS         = new ObjectId("6a5e67a6cc23ff9c6874a21c");
    private static final ObjectId ID_DELITO      = new ObjectId("6a5e67a6cc23ff9c6874a218");
    private static final ObjectId ID_INCIVILIDAD = new ObjectId("6a5e67a6cc23ff9c6874a219");

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }
        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");

        // ── 1. Asegurar las 3 Prioridades Estándar ──
        asegurarCatalogo(catalogos, ID_BAJA, "PRIORIDAD", "BAJA", "Baja", 1, "#3b82f6", "#eff6ff");
        asegurarCatalogo(catalogos, ID_MEDIA, "PRIORIDAD", "MEDIA", "Media", 2, "#f59e0b", "#fffbeb");
        asegurarCatalogo(catalogos, ID_ALTA, "PRIORIDAD", "ALTA", "Alta", 3, "#ef4444", "#fef2f2");

        // Desactivar cualquier prioridad no estándar
        catalogos.updateMany(
                Filters.and(
                        Filters.eq("tipo", "PRIORIDAD"),
                        Filters.nin("valor", List.of("BAJA", "MEDIA", "ALTA"))
                ),
                Updates.set("activo", false)
        );

        // ── 2. Asegurar las 3 Categorías Estándar ──
        asegurarCatalogo(catalogos, ID_SOS, "CATEGORIA", "SOS", "SOS", 5, "#6b21a8", "#faf5ff");
        asegurarCatalogo(catalogos, ID_DELITO, "CATEGORIA", "DELITO", "Delito", 4, "#14b8a6", "#f0fdfa");
        asegurarCatalogo(catalogos, ID_INCIVILIDAD, "CATEGORIA", "INCIVILIDAD", "Incivilidad", 2, "#008300", "#f0fdf4");

        // Desactivar cualquier categoría no estándar
        catalogos.updateMany(
                Filters.and(
                        Filters.eq("tipo", "CATEGORIA"),
                        Filters.nin("valor", List.of("SOS", "DELITO", "INCIVILIDAD"))
                ),
                Updates.set("activo", false)
        );
    }

    private void asegurarCatalogo(MongoCollection<Document> catalogos, ObjectId id,
                                  String tipo, String valor, String etiqueta,
                                  int nivelOrden, String colorHex, String colorBg) {
        Bson filtro = Filters.and(Filters.eq("tipo", tipo), Filters.eq("valor", valor));
        catalogos.updateOne(filtro, Updates.combine(
                Updates.set("etiqueta", etiqueta),
                Updates.set("nivelOrden", nivelOrden),
                Updates.set("colorHex", colorHex),
                Updates.set("colorBg", colorBg),
                Updates.set("activo", true),
                Updates.setOnInsert("_id", id),
                Updates.setOnInsert("tipo", tipo),
                Updates.setOnInsert("valor", valor)
        ), new UpdateOptions().upsert(true));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // En rollback se mantienen los catálogos base para no quebrar integridad
    }
}
