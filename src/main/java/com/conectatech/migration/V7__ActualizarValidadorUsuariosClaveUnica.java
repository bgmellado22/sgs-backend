package com.conectatech.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Arrays;

/**
 * Actualización del validador de esquema ($jsonSchema) de la colección "usuarios"
 * para compatibilidad total con la autenticación exclusiva de ClaveÚnica (OIDC).
 *
 * El atributo "password" deja de ser obligatorio en la persistencia de usuarios
 * registrados para el padrón municipal o ClaveÚnica.
 */
@ChangeUnit(id = "actualizar-validador-usuarios-claveunica", order = "007", author = "conectatech")
public class V7__ActualizarValidadorUsuariosClaveUnica {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("usuarios")) {
            mongoTemplate.createCollection("usuarios");
        }

        Document jsonSchema = new Document("bsonType", "object")
                .append("required", Arrays.asList("run", "email", "rol", "estado"))
                .append("properties", new Document()
                        .append("run", new Document("bsonType", "string")
                                .append("description", "RUN chileno con formato y dígito verificador - Requerido"))
                        .append("nombres", new Document("bsonType", "string"))
                        .append("primerApellido", new Document("bsonType", "string"))
                        .append("segundoApellido", new Document("bsonType", Arrays.asList("string", "null")))
                        .append("nombreCompleto", new Document("bsonType", "string"))
                        .append("email", new Document("bsonType", "string")
                                .append("pattern", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
                                .append("description", "Correo institucional único - Requerido"))
                        .append("password", new Document("bsonType", Arrays.asList("string", "null"))
                                .append("description", "Hash BCrypt de la contraseña - Opcional para usuarios ClaveÚnica"))
                        .append("rol", new Document("enum", Arrays.asList(
                                "ADMINISTRADOR", "OPERADOR", "INSPECTOR", "SUPERVISOR", "CIUDADANO"))
                                .append("description", "Rol de acceso en el sistema (RBAC) - Requerido"))
                        .append("estado", new Document("bsonType", "bool")
                                .append("description", "Estado de activación de la cuenta - Requerido"))
                        .append("intentos_fallidos", new Document("bsonType", Arrays.asList("int", "long"))
                                .append("description", "Contador de intentos de autenticación fallidos consecutivos"))
                        .append("bloqueado_hasta", new Document("bsonType", Arrays.asList("date", "null"))
                                .append("description", "Marca de tiempo de desbloqueo automático en caso de bloqueo por fuerza bruta"))
                );

        Document collModCmd = new Document("collMod", "usuarios")
                .append("validator", new Document("$jsonSchema", jsonSchema))
                .append("validationLevel", "moderate")
                .append("validationAction", "error");

        try {
            mongoTemplate.executeCommand(collModCmd);
        } catch (Exception e) {
            // Manejo defensivo en caso de entornos con esquemas de validación restringidos
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("usuarios")) {
            return;
        }

        Document jsonSchema = new Document("bsonType", "object")
                .append("required", Arrays.asList("run", "email", "password", "rol", "estado"))
                .append("properties", new Document()
                        .append("run", new Document("bsonType", "string"))
                        .append("email", new Document("bsonType", "string"))
                        .append("password", new Document("bsonType", "string"))
                        .append("rol", new Document("enum", Arrays.asList(
                                "ADMINISTRADOR", "OPERADOR", "INSPECTOR", "SUPERVISOR", "CIUDADANO")))
                        .append("estado", new Document("bsonType", "bool"))
                );

        Document collModCmd = new Document("collMod", "usuarios")
                .append("validator", new Document("$jsonSchema", jsonSchema))
                .append("validationLevel", "moderate")
                .append("validationAction", "error");

        try {
            mongoTemplate.executeCommand(collModCmd);
        } catch (Exception ignored) {
        }
    }
}
