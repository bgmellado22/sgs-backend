package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "estandarizar-tipo-incidentes", order = "006", author = "conectatech")
public class V6__EstandarizarTipoIncidentes {

        @Execution
        public void execution(MongoTemplate mongoTemplate) {
                if (!mongoTemplate.collectionExists("catalogos"))
                        return;

                // Robo -> DELITO, ALTA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor").is("ROBO_HURTO")),
                                new Update().set("categoriaAsociada", "DELITO").set("prioridadAsociada", "ALTA"),
                                "catalogos");

                // Incendio -> SOS, ALTA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor").is("INCENDIO")),
                                new Update().set("categoriaAsociada", "SOS").set("prioridadAsociada", "ALTA"),
                                "catalogos");

                // VIF -> DELITO, ALTA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor").is("VIF")),
                                new Update().set("categoriaAsociada", "DELITO").set("prioridadAsociada", "ALTA"),
                                "catalogos");

                // Consumo Drogas -> INCIVILIDAD, MEDIA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor")
                                                .is("CONSUMO_DROGAS")),
                                new Update().set("categoriaAsociada", "INCIVILIDAD").set("prioridadAsociada", "MEDIA"),
                                "catalogos");

                // Ruidos Molestos -> INCIVILIDAD, BAJA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor")
                                                .is("RUIDOS_MOLESTOS")),
                                new Update().set("categoriaAsociada", "INCIVILIDAD").set("prioridadAsociada", "BAJA"),
                                "catalogos");

                // Accidente -> SOS, ALTA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor")
                                                .is("ACCIDENTE_TRANSITO")),
                                new Update().set("categoriaAsociada", "SOS").set("prioridadAsociada", "ALTA"),
                                "catalogos");

                // Sospechoso -> INCIVILIDAD, MEDIA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor").is("SOSPECHOSO")),
                                new Update().set("categoriaAsociada", "INCIVILIDAD").set("prioridadAsociada", "MEDIA"),
                                "catalogos");

                // Comercio Ambulante -> INCIVILIDAD, BAJA
                mongoTemplate.updateFirst(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE").and("valor")
                                                .is("COMERCIO_AMBULANTE")),
                                new Update().set("categoriaAsociada", "INCIVILIDAD").set("prioridadAsociada", "BAJA"),
                                "catalogos");

                // OTRO -> No establecemos categoria ni prioridad, o lo dejamos null para que el
                // form lo detecte.
        }

        @RollbackExecution
        public void rollbackExecution(MongoTemplate mongoTemplate) {
                if (!mongoTemplate.collectionExists("catalogos"))
                        return;

                mongoTemplate.updateMulti(
                                new Query(Criteria.where("tipo").is("TIPO_INCIDENTE")),
                                new Update().unset("categoriaAsociada").unset("prioridadAsociada"),
                                "catalogos");
        }
}
