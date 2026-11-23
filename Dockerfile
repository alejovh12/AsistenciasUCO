# syntax=docker/dockerfile:1
#
# Build multi-stage: el stage "build" compila el codigo fuente ACTUAL con Maven Wrapper y produce
# un JAR nuevo dentro del propio build de Docker. El stage final solo copia ese JAR recien
# generado. Esto evita el riesgo de empaquetar silenciosamente un target/*.jar desactualizado que
# ya no corresponda al codigo fuente (ver docs del backend sobre "JAR desactualizado").
# El starter de MessageUcoLab vive en GitHub Packages (repositorio privado del equipo de mensajeria). Las credenciales
# entran como secreto de BuildKit (docker build --secret id=m2settings,src=<settings.xml>) y no quedan en ninguna capa
# de la imagen. El secreto es opcional (required=false): sin el, el build solo falla si realmente necesita el starter.
# F12-02: etiqueta + digest (reproducible); Dependabot (ecosistema docker en "/") actualiza ambos.
FROM eclipse-temurin:25-jdk-alpine@sha256:c7c3d0bdf7f2bbc5a05a6712ed334d198360105634ea9adf76fca94f381b1e3f AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=secret,id=m2settings,target=/root/.m2/settings.xml,required=false chmod +x mvnw && ./mvnw -U -B -ntp dependency:go-offline

COPY src/ src/
COPY docs/contracts/openapi/openapi-golden-path.yaml docs/contracts/openapi/openapi-golden-path.yaml
COPY docs/contracts/openapi/openapi-golden-path.sha256 docs/contracts/openapi/openapi-golden-path.sha256
# AccessCatalogConsistencyTest contrasta Spring MVC y SecurityConfig con el catalogo de acceso compartido.
COPY infra/security-baseline/access-catalog.json infra/security-baseline/access-catalog.json
# RuntimeSecretCatalogTest (SEC-02) contrasta el catalogo de secretos con el inventario documentado.
COPY docs/security/SECRETS_INVENTORY.md docs/security/SECRETS_INVENTORY.md
# OperationalReadinessConfigurationTest (F12-09) lee el propio Dockerfile para verificar HEALTHCHECK y ENTRYPOINT.
COPY Dockerfile Dockerfile
RUN --mount=type=secret,id=m2settings,target=/root/.m2/settings.xml,required=false ./mvnw -U -B -ntp clean verify
# El JAR se desempaqueta por capas (jarmode=tools de Spring Boot): las 244 librerias (~172 MB) quedan en una capa que solo
# cambia cuando cambia el pom, y el codigo propio (~5 MB) en otra. Un cambio de codigo ya no reescribe ~185 MB de imagen.
# De paso las clases salen de JAR normales del classpath (no anidados), requisito para la cache de clases de mas abajo.
RUN cp target/AsistenciasUCO-0.0.1-SNAPSHOT.jar app.jar     && java -Djarmode=tools -jar app.jar extract --layers --destination extraido

FROM eclipse-temurin:25-jre-alpine@sha256:e1898fd6a11fc965a994d8ace186fd3585aeb636cf8f05f47a723fcfee0b0918
WORKDIR /app
# Orden de menor a mayor frecuencia de cambio; cada capa aporta lib/ o el app.jar delgado (su manifiesto apunta a lib/).
COPY --from=build /workspace/extraido/dependencies/ ./
COPY --from=build /workspace/extraido/spring-boot-loader/ ./
COPY --from=build /workspace/extraido/snapshot-dependencies/ ./
COPY --from=build /workspace/extraido/application/ ./
# SEC-002: Alpine CVE-2026-46675 (libpng), CVE-2026-85091 (zlib), CVE-2026-109908 (libfontenc).
# Update OS packages in the runtime stage, never merely in the builder stage.
# /app/cache: unico directorio donde el usuario sin privilegios escribe la cache de clases (ver ENTRYPOINT).
RUN apk upgrade --no-cache libpng zlib libfontenc     && mkdir -p /app/cache && chown 10001:10001 /app/cache
# SEC-002 defense-in-depth: production JVM runs without root privileges.
# JAR is copied as world-readable and Java can use /tmp on Alpine.
USER 10001:10001
EXPOSE 8080
# F12-09 / OBS-10: Actuator vive en el puerto de gestion (MANAGEMENT_SERVER_PORT, 8090 por defecto) y escucha en loopback
# dentro del contenedor, asi que el healthcheck local llega sin exponer ese puerto. Se usa la sonda de readiness (BD lista y
# contexto arrancado); wget es el de BusyBox de la imagen Alpine. start-period cubre el arranque de Spring y la carga de
# configuracion externa.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["sh", "-c", "wget -q -O /dev/null \"http://127.0.0.1:${MANAGEMENT_SERVER_PORT:-8090}/actuator/health/readiness\" || exit 1"]
# F12-09: exec form => la JVM es el PID 1 y recibe SIGTERM directamente (apagado elegante de server.shutdown=graceful).
# Cache de clases (CDS dinamico): el primer arranque la escribe al apagarse y los siguientes la reutilizan; si no coincide
# con la imagen (classpath distinto) la JVM la descarta y la vuelve a crear. Medido en el laboratorio: arranque de ~22 s a
# ~12 s y ~60 MB menos en reposo. No se puede generar en el build porque la aplicacion exige su configuracion y la BD para
# arrancar (fail-closed). Sin volumen en /app/cache dura lo que el contenedor; con sistema de archivos de solo lectura la
# JVM solo avisa y arranca sin cache.
ENTRYPOINT ["java", "-XX:+AutoCreateSharedArchive", "-XX:SharedArchiveFile=/app/cache/asistencias.jsa", "-jar", "app.jar"]
