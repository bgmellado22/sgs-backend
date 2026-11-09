package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Inicialización integral y definitiva de catálogos maestros del sistema SGS El Tabo.
 * Incluye: Prioridades, Categorías, Orígenes, Tipos de Incidentes (con relaciones),
 * Canales de Enlace y Tipos de Patrullaje con sus colores, jerarquías y versionamiento optimista.
 */
@ChangeUnit(id = "init-catalogos", order = "001", author = "conectatech")
public class V1__InitCatalogos {

    // IDs fijos estándar de Prioridades y Categorías
    private static final ObjectId ID_PRIORIDAD_BAJA   = new ObjectId("6a5e67a6cc23ff9c6874a21d");
    private static final ObjectId ID_PRIORIDAD_MEDIA  = new ObjectId("6a5e67a6cc23ff9c6874a21b");
    private static final ObjectId ID_PRIORIDAD_ALTA   = new ObjectId("6a5e67a6cc23ff9c6874a21a");

    private static final ObjectId ID_CATEGORIA_INCIVILIDAD = new ObjectId("6a5e67a6cc23ff9c6874a219");
    private static final ObjectId ID_CATEGORIA_DELITO      = new ObjectId("6a5e67a6cc23ff9c6874a218");
    private static final ObjectId ID_CATEGORIA_SOS         = new ObjectId("6a5e67a6cc23ff9c6874a21c");

    // IDs fijos estándar de Orígenes
    private static final ObjectId ID_ORIGEN_LLAMADA  = new ObjectId("6a5e949acc23ff9c6874a238");
    private static final ObjectId ID_ORIGEN_CAMARA   = new ObjectId("6a5e949acc23ff9c6874a239");
    private static final ObjectId ID_ORIGEN_PATRULLA = new ObjectId("6a5e949acc23ff9c6874a23a");

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.createCollection("catalogos");
        }

        if (mongoTemplate.getCollection("catalogos").countDocuments() == 0) {
            List<Document> catalogos = new ArrayList<>();

            // 1. PRIORIDADES (Escala térmica: Azul, Ámbar, Rojo)
            catalogos.add(crearDocumentoCatalogo(ID_PRIORIDAD_BAJA, "PRIORIDAD", "BAJA", "Baja", 1, "#3b82f6", "#eff6ff", null, null));
            catalogos.add(crearDocumentoCatalogo(ID_PRIORIDAD_MEDIA, "PRIORIDAD", "MEDIA", "Media", 2, "#f59e0b", "#fffbeb", null, null));
            catalogos.add(crearDocumentoCatalogo(ID_PRIORIDAD_ALTA, "PRIORIDAD", "ALTA", "Alta", 3, "#ef4444", "#fef2f2", null, null));

            // 2. CATEGORÍAS (Colores distintivos de identidad)
            catalogos.add(crearDocumentoCatalogo(ID_CATEGORIA_INCIVILIDAD, "CATEGORIA", "INCIVILIDAD", "Incivilidad", 2, "#008300", "#f0fdf4", null, null));
            catalogos.add(crearDocumentoCatalogo(ID_CATEGORIA_DELITO, "CATEGORIA", "DELITO", "Delito", 4, "#14b8a6", "#f0fdfa", null, null));
            catalogos.add(crearDocumentoCatalogo(ID_CATEGORIA_SOS, "CATEGORIA", "SOS", "SOS", 5, "#6b21a8", "#faf5ff", null, null));

            // 3. ORÍGENES DE DENUNCIA
            catalogos.add(crearDocumentoCatalogo(ID_ORIGEN_LLAMADA, "ORIGEN", "LLAMADA", "Llamada Telefónica", null, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(ID_ORIGEN_CAMARA, "ORIGEN", "CAMARA_SEGURIDAD", "Cámara de Seguridad", null, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(ID_ORIGEN_PATRULLA, "ORIGEN", "PATRULLA", "Patrulla en Terreno", null, null, null, null, null));

            // 4. TIPOS DE INCIDENTE (con categorización y prioridad asociada por defecto)
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "ROBO_HURTO", "Robo", null, null, null, "DELITO", "ALTA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "INCENDIO", "Incendio", null, null, null, "SOS", "ALTA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "VIF", "Violencia Intrafamiliar (VIF)", null, null, null, "DELITO", "ALTA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "CONSUMO_DROGAS", "Consumo de Alcohol/Drogas en Vía Pública", null, null, null, "INCIVILIDAD", "MEDIA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "RUIDOS_MOLESTOS", "Ruidos Molestos", null, null, null, "INCIVILIDAD", "BAJA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "ACCIDENTE_TRANSITO", "Accidente de Tránsito", null, null, null, "SOS", "ALTA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "SOSPECHOSO", "Persona Sospechosa", null, null, null, "INCIVILIDAD", "MEDIA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "COMERCIO_AMBULANTE", "Comercio Ambulante no Autorizado", null, null, null, "INCIVILIDAD", "BAJA"));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_INCIDENTE", "OTRO", "Otro (Especificar en descripción)", null, null, null, null, null));

            // 5. CANALES DE ENLACE (Derivación Carabineros)
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "CANAL_ENLACE", "RADIAL_CENCO", "Frecuencia Radial CENCO", 1, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "CANAL_ENLACE", "TELEFONO_CUADRANTE", "Teléfono Directo Cuadrante", 2, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "CANAL_ENLACE", "CORREO_INSTITUCIONAL", "Correo Institucional", 3, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "CANAL_ENLACE", "OFICIO_CONDUCTOR", "Oficio Conductor", 4, null, null, null, null));

            // 6. TIPOS DE PATRULLAJE PREVENTIVO
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_PATRULLAJE", "PREVENTIVO", "Patrullaje Preventivo", 1, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_PATRULLAJE", "BORDE_COSTERO", "Patrullaje Borde Costero", 2, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_PATRULLAJE", "ESCOLAR", "Patrullaje Escolar", 3, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_PATRULLAJE", "FOCALIZADO", "Patrullaje Focalizado", 4, null, null, null, null));
            catalogos.add(crearDocumentoCatalogo(new ObjectId(), "TIPO_PATRULLAJE", "NOCTURNO", "Patrullaje Nocturno", 5, null, null, null, null));

            mongoTemplate.getCollection("catalogos").insertMany(catalogos);
        }
    }

    private Document crearDocumentoCatalogo(ObjectId id, String tipo, String valor, String etiqueta,
                                           Integer nivelOrden, String colorHex, String colorBg,
                                           String categoriaAsociada, String prioridadAsociada) {
        Document doc = new Document("_id", id)
                .append("tipo", tipo)
                .append("valor", valor)
                .append("etiqueta", etiqueta)
                .append("activo", true)
                .append("version", 0L);

        if (nivelOrden != null) {
            doc.append("nivelOrden", nivelOrden);
        }
        if (colorHex != null) {
            doc.append("colorHex", colorHex);
        }
        if (colorBg != null) {
            doc.append("colorBg", colorBg);
        }
        if (categoriaAsociada != null) {
            doc.append("categoriaAsociada", categoriaAsociada);
        }
        if (prioridadAsociada != null) {
            doc.append("prioridadAsociada", prioridadAsociada);
        }
        return doc;
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (mongoTemplate.collectionExists("catalogos")) {
            mongoTemplate.dropCollection("catalogos");
        }
    }
}
