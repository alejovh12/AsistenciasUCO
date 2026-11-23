# syntax=docker/dockerfile:1
#
# Servicio de Notificaciones UCO 0.1.1 (Spring Boot 4 / Java 25). Build multi-stage: el stage "build" compila el codigo
# fuente ACTUAL con Maven Wrapper (sin pruebas: las pruebas de integracion usan Testcontainers y se corren fuera con
# `mvnw verify`) y el stage final solo copia lo empaquetado, por capas, y corre sin privilegios.
# Mismas imagenes base (etiqueta + digest) que el backend de Asistencias.
FROM eclipse-temurin:25-jdk-alpine@sha256:c7c3d0bdf7f2bbc5a05a6712ed334d198360105634ea9adf76fca94f381b1e3f AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests clean package
# Capas del JAR (jarmode=tools): las librerias quedan en una capa que solo cambia cuando cambia el pom.
RUN cp target/notificaciones-uco.jar app.jar && java -Djarmode=tools -jar app.jar extract --layers --destination extraido

FROM eclipse-temurin:25-jre-alpine@sha256:e1898fd6a11fc965a994d8ace186fd3585aeb636cf8f05f47a723fcfee0b0918
WORKDIR /app
COPY --from=build /workspace/extraido/dependencies/ ./
COPY --from=build /workspace/extraido/spring-boot-loader/ ./
COPY --from=build /workspace/extraido/snapshot-dependencies/ ./
COPY --from=build /workspace/extraido/application/ ./
# Parches del SO en la etapa final (no basta con la del build) y directorio de la cache de clases para el usuario sin privilegios.
RUN apk upgrade --no-cache && mkdir -p /app/cache && chown 10001:10001 /app/cache
USER 10001:10001
EXPOSE 8080
# Readiness = BD lista y contexto arrancado (el SMTP no cuenta: una caida del correo no saca al servicio del trafico).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD ["sh", "-c", "wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1"]
# Exec form: la JVM es el PID 1 y recibe SIGTERM (apagado elegante). Cache de clases (CDS dinamico) en /app/cache.
ENTRYPOINT ["java", "-XX:+AutoCreateSharedArchive", "-XX:SharedArchiveFile=/app/cache/notificaciones.jsa", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
