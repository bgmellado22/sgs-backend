# Compilación con JDK 17 y Gradle
FROM gradle:8.7-jdk17 AS build
WORKDIR /app

# Copiamos los archivos del proyecto
COPY . .

# Otorgamos permisos de ejecución a gradlew antes de compilar
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test --no-daemon

# Imagen de ejecución (Debian, no Alpine, para compatibilidad con mongodb+srv DNS)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
# Zona horaria de Chile: LocalDateTime.now() y las fechas que se envían al frontend quedan en hora local
ENTRYPOINT ["java", "-Duser.timezone=America/Santiago", "-jar", "app.jar"]