# syntax=docker/dockerfile:1
#
# Build multi-stage: el stage "build" compila el codigo fuente ACTUAL con Maven Wrapper y produce
# un JAR nuevo dentro del propio build de Docker. El stage final solo copia ese JAR recien
# generado. Esto evita el riesgo de empaquetar silenciosamente un target/*.jar desactualizado que
# ya no corresponda al codigo fuente (ver docs del backend sobre "JAR desactualizado").
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean verify

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/AsistenciasUCO-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
