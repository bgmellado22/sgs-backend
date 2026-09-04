# Sistema de Gestión de Seguridad (SGS) - Backend API Restful [![My Skills](https://skillicons.dev/icons?i=java,mongodb)](https://skillicons.dev)

**Asignatura:** Lenguaje de Programación Web II

**Caso de negocio:** Sistema de Gestión de Seguridad para Municipalidad de El Tabo

API RESTful desarrollada para la gestión operativa y seguimiento de incidencias de seguridad comunal. Este proyecto backend implementa una arquitectura limpia por capas, versionamiento de endpoints, persistencia de datos NoSQL con migraciones aplicadas, y un robusto blindaje de seguridad basado en JWT.

---

## Stack Tecnológico y Arquitectura

* **Lenguaje y Framework:** Java 17 + Spring Boot 3.x
* **Gestor de Dependencias:** Gradle
* **Persistencia (ORM Equivalente):** Spring Data MongoDB
* **Migraciones de BD:** Mongock (ChangeUnits)
* **Seguridad:** Spring Security + JWT
* **Documentación:** Swagger UI
* **Testing:** JUnit 5 + MockMvc

El proyecto sigue una Arquitectura por capas (Controller - Service - Repository) utilizando el patrón DTO (Data Transfer Object) para aislar el modelo de dominio y evitar la sobreexposición de datos. La inyección de dependencias se maneja por constructor mediante Lombok.

---

## Seguridad y Mitigación OWASP API Top 10
El ecosistema cuenta con un filtro y un control de acceso basado en roles. Se han mitigado las siguientes vulnerabilidades:

1.  **API1:2023 (Broken Object Level Authorization):** Implementación de directivas RBAC (`.hasAnyRole("ADMINISTRADOR", "OPERADOR")`) en el `SecurityFilterChain`.
2.  **API2:2023 (Broken Authentication):** Autenticación *stateless* con tokens JWT firmados criptográficamente (HMAC-SHA256) y validación de expiración.
3.  **API3:2023 (Broken Object Property Level Authorization):** Uso estricto de `RequestDTO` y `ResponseDTO` para evitar la serialización de datos sensibles del usuario.
4.  **API8:2023 (Security Misconfiguration):** Desactivación del *Whitelabel Error Page* mediante un `GlobalExceptionHandler` (`@RestControllerAdvice`) para no filtrar *stack traces* al cliente.
5.  **APIX (Inyección NoSQL):** Uso de repositorios de Spring Data (`MongoRepository`) que gestionan consultas mediante parámetros tipados, neutralizando concatenaciones maliciosas.

Se incluye un Middleware propio que intercepta y audita los tiempos de respuesta y códigos de estado HTTP de todas las peticiones entrantes.

---

## Instalación y Ejecución Local

### 1. Clonar el repositorio (Rama: rama-web2)
```bash
git clone -b rama-web2 [https://github.com/bgmellado22/sgs-backend.git](https://github.com/bgmellado22/sgs-backend.git)
cd sgs-backend
```

### 2. Variables de entorno
El proyecto está diseñado para no exponer secretos. Antes de ejecutar, configura las siguientes variables de entorno en tu entorno local:
* **PORT** (Opcional, por defecto 8080)
* **SPRING_DATA_MONGODBURI** (Ej: mongodb://localhost:27017/sgs_db)
* **JWT_SECRET** (Llave secreta de 256 bits)

### 3. Ejecutar la aplicación
Para levantar el servidor web y ejecutar automáticamente las migraciones de Mongock:
```bash
./gradlew bootRun
```

---

## Documentación Técnica y Pruebas

* **Contrato OpenAPI (Swagger):** Una vez que el servidor esté corriendo, la documentación interactiva de los endpoints versionados estará disponible de forma pública en: `http://localhost:8080/swagger`
* **Suite de Pruebas de Integración:** El proyecto incluye pruebas automatizadas para validar rutas, códigos de estado HTTP y compatibilidad de JSON. Se ejecutan con:
```bash
./gradlew test
```
