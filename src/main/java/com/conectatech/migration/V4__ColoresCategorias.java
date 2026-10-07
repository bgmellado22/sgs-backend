package com.conectatech.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Categorías con un color distinto cada una (identidad), en vez de tonos de un mismo color por gravedad.
 * El nivel de gravedad (nivelOrden) se conserva: ahora solo ordena, no define el color.
 *
 * Fuera del espectro térmico de prioridades caben 4 colores distinguibles; desde la quinta
 * categoría se asigna el neutro (el frontend las agrupa en "Otras").
 * Debe mantenerse sincronizado con src/utils/paletaCatalogos.js del frontend.
 */
@ChangeUnit(id = "colores-categorias-distintos", order = "004", author = "conectatech")
public class V4__ColoresCategorias {

    private record Color(String hex, String bg) {}

    private static final Color PURPURA = new Color("#6b21a8", "#faf5ff");
    private static final Color FUCSIA = new Color("#c026d3", "#fdf4ff");
    private static final Color VERDE = new Color("#008300", "#f0fdf4");
    private static final Color TURQUESA = new Color("#14b8a6", "#f0fdfa");
    private static final Color NEUTRO = new Color("#94a3b8", "#f1f5f9");

    private static final List<Color> PALETA = List.of(PURPURA, FUCSIA, VERDE, TURQUESA);

    // IDs fijos de V1 (DELITO, INCIVILIDAD) y V2 (SOS)
    private static final ObjectId ID_DELITO = new ObjectId("6a5e67a6cc23ff9c6874a218");
    private static final ObjectId ID_INCIVILIDAD = new ObjectId("6a5e67a6cc23ff9c6874a219");
    private static final ObjectId ID_SOS = new ObjectId("6a5e67a6cc23ff9c6874a21c");

    // Colores de las categorías base antes de esta migración (para el rollback)
    private static final Color SOS_ANTERIOR = new Color("#581c87", "#f3e8ff");
    private static final Color DELITO_ANTERIOR = new Color("#7e22ce", "#f3e8ff");
    private static final Color INCIVILIDAD_ANTERIOR = new Color("#c084fc", "#faf5ff");

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");
        Set<Object> asignadas = new HashSet<>();
        List<Color> libres = new ArrayList<>(PALETA);

        // 1. Categorías base: SOS púrpura, Delito fucsia, Incivilidad verde
        asignarBase(catalogos, ID_SOS, "SOS", PURPURA, asignadas, libres);
        asignarBase(catalogos, ID_DELITO, "DELITO", FUCSIA, asignadas, libres);
        asignarBase(catalogos, ID_INCIVILIDAD, "INCIVILIDAD", VERDE, asignadas, libres);

        // 2. Resto de categorías (creadas por el administrador): de más a menos grave,
        //    reciben los colores que queden libres; las demás, el neutro
        List<Document> otras = catalogos.find(Filters.eq("tipo", "CATEGORIA"))
                .sort(Sorts.orderBy(Sorts.descending("nivelOrden"), Sorts.ascending("etiqueta")))
                .into(new ArrayList<>());

        for (Document categoria : otras) {
            if (asignadas.contains(categoria.get("_id"))) continue;
            Color color = libres.isEmpty() ? NEUTRO : libres.remove(0);
            catalogos.updateOne(Filters.eq("_id", categoria.get("_id")), aplicar(color));
        }
    }

    /**
     * Busca la categoría base por su ID fijo o, si el administrador la recreó, por su valor.
     * Si fue eliminada no se recrea y su color queda libre para otra categoría.
     */
    private void asignarBase(MongoCollection<Document> catalogos, ObjectId id, String valor, Color color,
                             Set<Object> asignadas, List<Color> libres) {
        Document categoria = catalogos.find(Filters.eq("_id", id)).first();
        if (categoria == null) {
            categoria = catalogos.find(Filters.and(Filters.eq("tipo", "CATEGORIA"), Filters.eq("valor", valor))).first();
        }
        if (categoria == null) return;

        catalogos.updateOne(Filters.eq("_id", categoria.get("_id")), aplicar(color));
        asignadas.add(categoria.get("_id"));
        libres.remove(color);
    }

    private org.bson.conversions.Bson aplicar(Color color) {
        return Updates.combine(Updates.set("colorHex", color.hex()), Updates.set("colorBg", color.bg()));
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // Restaura los colores de V2 en las categorías base; las creadas por el administrador
        // conservan el color asignado (no hay un valor anterior conocido al que volver)
        MongoCollection<Document> catalogos = mongoTemplate.getCollection("catalogos");
        catalogos.updateOne(Filters.eq("_id", ID_SOS), aplicar(SOS_ANTERIOR));
        catalogos.updateOne(Filters.eq("_id", ID_DELITO), aplicar(DELITO_ANTERIOR));
        catalogos.updateOne(Filters.eq("_id", ID_INCIVILIDAD), aplicar(INCIVILIDAD_ANTERIOR));
    }
}
