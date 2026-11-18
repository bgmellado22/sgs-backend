package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Arrays;

/**
 * Migración V8: Incorporación del campo 'disponible' en la colección 'usuarios'
 * para la gestión de disponibilidad en turno de inspectores y operadores
 * tácticos.
 */
@ChangeUnit(id = "campo-disponibilidad-inspector", order = "008", author = "conectatech")
public class V8__CampoDisponibilidadInspector {

        @Execution
        public void execution(MongoTemplate mongoTemplate) {
                if (!mongoTemplate.collectionExists("usuarios")) {
                        mongoTemplate.createCollection("usuarios");
                }

                // Actualizar inspectores existentes con valor por defecto disponible: false si
                // es nulo
                Query query = new Query(Criteria.where("disponible").is(null));
                Update update = new Update().set("disponible", false);
                mongoTemplate.updateMulti(query, update, "usuarios");

                // Actualizar el esquema con el atributo disponible
                Document jsonSchema = new Document("bsonType", "object")
                                .append("required", Arrays.asList("run", "email", "rol", "estado"))
                                .append("properties", new Document()
                                                .append("run", new Document("bsonType", "string")
                                                                .append("description",
                                                                                "RUN chileno con formato y dígito verificador - Requerido"))
                                                .append("nombres", new Document("bsonType", "string"))
                                                .append("primerApellido", new Document("bsonType", "string"))
                                                .append("segundoApellido",
                                                                new Document("bsonType",
                                                                                Arrays.asList("string", "null")))
                                                .append("nombreCompleto", new Document("bsonType", "string"))
                                                .append("email", new Document("bsonType", "string")
                                                                .append("pattern",
                                                                                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
                                                                .append("description",
                                                                                "Correo institucional único - Requerido"))
                                                .append("password", new Document("bsonType",
                                                                Arrays.asList("string", "null"))
                                                                .append("description",
                                                                                "Hash BCrypt de la contraseña - Opcional para usuarios ClaveÚnica"))
                                                .append("rol", new Document("enum", Arrays.asList(
                                                                "ADMINISTRADOR", "OPERADOR", "INSPECTOR", "SUPERVISOR",
                                                                "CIUDADANO"))
                                                                .append("description",
                                                                                "Rol de acceso en el sistema (RBAC) - Requerido"))
                                                .append("estado", new Document("bsonType", "bool")
                                                                .append("description",
                                                                                "Estado de activación de la cuenta - Requerido"))
                                                .append("disponible", new Document("bsonType",
                                                                Arrays.asList("bool", "null"))
                                                                .append("description",
                                                                                "Disponibilidad táctica en turno para asignación de patrulla"))
                                                .append("intentos_fallidos", new Document("bsonType",
                                                                Arrays.asList("int", "long"))
                                                                .append("description",
                                                                                "Contador de intentos de autenticación fallidos consecutivos"))
                                                .append("bloqueado_hasta", new Document("bsonType",
                                                                Arrays.asList("date", "null"))
                                                                .append("description",
                                                                                "Marca de tiempo de desbloqueo automático en caso de bloqueo por fuerza bruta")));

                Document collModCmd = new Document("collMod", "usuarios")
                                .append("validator", new Document("$jsonSchema", jsonSchema))
                                .append("validationLevel", "moderate")
                                .append("validationAction", "error");

                try {
                        mongoTemplate.executeCommand(collModCmd);
                } catch (Exception ignored) {
                }
        }

        @RollbackExecution
        public void rollbackExecution(MongoTemplate mongoTemplate) {
                if (!mongoTemplate.collectionExists("usuarios")) {
                        return;
                }

                Query query = new Query(Criteria.where("disponible").exists(true));
                Update update = new Update().unset("disponible");
                mongoTemplate.updateMulti(query, update, "usuarios");
        }
}
