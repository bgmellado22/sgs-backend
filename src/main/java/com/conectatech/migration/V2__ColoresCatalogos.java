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

// order "002": agrega jerarquía y colores a los catálogos base (Semana 17)
@ChangeUnit(id = "colores-catalogos", order = "002", author = "conectatech")
public class V2__ColoresCatalogos {

    // IDs fijos de V1__InitCatalogos
    private static final ObjectId ID_DELITO      = new ObjectId("6a5e67a6cc23ff9c6874a218");
    private static final ObjectId ID_INCIVILIDAD = new ObjectId("6a5e67a6cc23ff9c6874a219");
    private static final ObjectId ID_ALTA        = new ObjectId("6a5e67a6cc23ff9c6874a21a");
    private static final ObjectId ID_MEDIA       = new ObjectId("6a5e67a6cc23ff9c6874a21b");

    // IDs fijos para los nuevos valores base
    private static final ObjectId ID_SOS  = new ObjectId("6a5e67a6cc23ff9c6874a21c");
    private static final ObjectId ID_BAJA = new ObjectId("6a5e67a6cc23ff9c6874a21d");

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");

        // ── Prioridades: escala térmica (azul, ámbar, rojo) ──
        actualizarPorId(catalogos, ID_MEDIA, 2, "#f59e0b", "#fffbeb");
        actualizarPorId(catalogos, ID_ALTA,  3, "#ef4444", "#fef2f2");
        crearSiNoExiste(catalogos, ID_BAJA, "PRIORIDAD", "BAJA", "Baja", 1, "#3b82f6", "#eff6ff");

        // ── Categorías: semilla púrpura, escala cerrada según gravedad ──
        actualizarPorId(catalogos, ID_INCIVILIDAD, 2, "#c084fc", "#faf5ff");
        actualizarPorId(catalogos, ID_DELITO,      4, "#7e22ce", "#f3e8ff");
        crearSiNoExiste(catalogos, ID_SOS, "CATEGORIA", "SOS", "SOS", 5, "#581c87", "#f3e8ff");
    }

    /**
     * Asigna jerarquía y colores a un catálogo de V1. Si el administrador
     * lo eliminó, no se vuelve a crear.
     */
    private void actualizarPorId(MongoCollection<Document> catalogos, ObjectId id,
                                 int nivelOrden, String colorHex, String colorBg) {
        catalogos.updateOne(Filters.eq("_id", id), Updates.combine(
                Updates.set("nivelOrden", nivelOrden),
                Updates.set("colorHex", colorHex),
                Updates.set("colorBg", colorBg)
        ));
    }

    /**
     * Inserta un valor base nuevo. Si el administrador ya lo había creado
     * manualmente (mismo tipo y valor), solo se le asignan los colores.
     */
    private void crearSiNoExiste(MongoCollection<Document> catalogos, ObjectId id,
                                 String tipo, String valor, String etiqueta,
                                 int nivelOrden, String colorHex, String colorBg) {
        Bson filtro = Filters.and(Filters.eq("tipo", tipo), Filters.eq("valor", valor));
        catalogos.updateOne(filtro, Updates.combine(
                Updates.set("nivelOrden", nivelOrden),
                Updates.set("colorHex", colorHex),
                Updates.set("colorBg", colorBg),
                Updates.setOnInsert("_id", id),
                Updates.setOnInsert("etiqueta", etiqueta),
                Updates.setOnInsert("activo", true)
        ), new UpdateOptions().upsert(true));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");

        // Eliminar solo los documentos insertados por este script
        catalogos.deleteMany(Filters.in("_id", List.of(ID_SOS, ID_BAJA)));

        // Quitar los atributos visuales de los catálogos base
        catalogos.updateMany(
                Filters.or(
                        Filters.in("_id", List.of(ID_DELITO, ID_INCIVILIDAD, ID_ALTA, ID_MEDIA)),
                        Filters.and(Filters.eq("tipo", "PRIORIDAD"), Filters.eq("valor", "BAJA")),
                        Filters.and(Filters.eq("tipo", "CATEGORIA"), Filters.eq("valor", "SOS"))
                ),
                Updates.combine(
                        Updates.unset("nivelOrden"),
                        Updates.unset("colorHex"),
                        Updates.unset("colorBg")
                )
        );
    }
}
