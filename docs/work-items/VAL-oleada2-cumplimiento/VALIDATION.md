---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# VAL-oleada2-cumplimiento - VALIDATION

## Cadena de suministro y CI (agente T)

actionlint 0 hallazgos; YAML OK; Trivy local 3 HIGH reales (guava, snappy-java, transitivas de minio); gitleaks con configuración 0 hallazgos. Detalle en [CI-CADENA-SUMINISTRO.md](CI-CADENA-SUMINISTRO.md).

## Agente B: ROL-06, F12-09, OBS-10, REA-02, PAR-05, PAR-06, CF-05, CF-06, F12-11 y CVE de minio

Fecha: 2026-11-12. JDK 25.0.5, `.\mvnw.cmd -B -o test -Dtest=... -Djacoco.skip=true` (sin `clean verify`, por memoria).

| Corrida | Alcance (`-Dtest`) | Resultado |
|---|---|---|
| 1 | `architecture.**`, `infrastructure.adapter.primary.**`, `adapter.secondary.{securitybaseline,storage,malwarescan}.**`, `infrastructure.config.**`, `application.features.{archivo,consultastransversales}.**`, sin `*IT` | 1490 pruebas, 9 fallos: 1 de UUID nulo en `AdminPortalControllerTest` (se agregó `@NonEmptyUuid`), 1 de carrera en `RealtimeStreamErrorHttpTest`, 1 de `FileProvidersConfigurationTest` (ahora publica el decorador) y 6 de `OperationalReadinessConfigurationTest` (leía el `application.yml` de test; ahora lee el de `src/main/resources`). Todos corregidos. |
| 2 | Las clases que fallaron + `BeanValidationControllersTest`, `MinioClientTimeoutBoundaryTest`, controladores `archivo`, `asistencia`, `coordinador`, `sesion`, `docente`, `adapter.secondary.{storage,malwarescan}.**`, `application.features.archivo.**` (ya con guava 33.7.2 y snappy 1.1.10.12) | 413 pruebas, 1 fallo en `RealtimeStreamErrorHttpTest.grupoId_en_mayusculas...` (leía el evento antes de que llegara su `data:`; se corrigió la espera). |
| 3 | `RealtimeStreamErrorHttpTest` tres veces | 7/7 en cada corrida. |
| 4 | `architecture.**`, `adapter.primary.security.**`, `infrastructure.config.**`, controladores `admin`, `identidad`, `decano`, `validation`, `error`, `adapter.secondary.securitybaseline.**`, `application.features.consultastransversales.**` | **850 pruebas, 0 fallos, BUILD SUCCESS.** |

Pruebas nuevas:
- `controller/validation/BeanValidationControllersTest` (ROL-06): toma (lote vacío, cascada por registro, modo vacío sigue en Application), asignaturas del plan (campos obligatorios en orden, rango, mapeo al DTO), portal del coordinador (`inp`, longitud de código), retiro (motivo > 500), verificación de correo (token desmedido), payload de código y ausencia de mensajes del proveedor.
- `adapter/secondary/securitybaseline/SecurityBaselinePepCircuitBreakerTest` (REA-02): el breaker abre con el PEP caído o inalcanzable; abierto responde `UNAVAILABLE` (503) sin llamar al PEP y en menos de 500 ms; 401/403 no lo abren; sin Bearer no lo toca.
- `adapter/secondary/storage/resilience/CircuitBreakerFileStorageAdapterTest` y `adapter/secondary/malwarescan/resilience/CircuitBreakerMalwareScanAdapterTest` (REA-02): abre y falla cerrado; `ObjectNotFound` e `INFECTED` no cuentan; errores de programación no cuentan.
- `config/OperationalReadinessConfigurationTest` (F12-09, OBS-10, F12-11, CF-05, REA-02): claves del `application.yml`, `HEALTHCHECK` del Dockerfile y breakers construidos por la autoconfiguración de Resilience4j con los valores del YAML.
- `SecurityConfigTest` (CF-05, CF-06): CSP, `X-Frame-Options: DENY`, nosniff, Referrer-Policy y Permissions-Policy en 200 y 401; HSTS solo en HTTPS; CSP propia de Swagger UI; preflight de origen rechazado sin encabezados CORS.
- `SubirArchivoTamanioParametrizadoTest` (PAR-05) y `ConsultasAuditoriaRangoParametrizadoTest` (PAR-06).

Dependencias: `mvnw dependency:tree -Dincludes=com.google.guava:guava,org.xerial.snappy:snappy-java` → `guava:33.7.2-jre`, `snappy-java:1.1.10.12`.

No corrido aquí: `clean verify` completo (JaCoCo, CodeQL, Sonar), `-Pintegration` y los `*IT` (`ArchivoUploadDownloadFlowIT` necesita MinIO y ClamAV reales), arranque real del contenedor con el `HEALTHCHECK`.

### Propuesta de migración para la BD (no se tocó la BD)

PAR-06 necesita este parámetro, que no existe en el seed (mientras tanto el código usa 90 días):

| grupo | clave | valor | tipo | rango sugerido para la edición |
|---|---|---|---|---|
| `AUDITORIA` | `RANGO_MAXIMO_DIAS` | `90` | `INT` | 1..366 (el código ignora valores fuera de ese rango) |

`AUDITORIA` está en `PARAMETROS/GRUPOS_NO_EDITABLES`: si se quiere editable desde la pantalla de parámetros hay que sacarlo de esa lista o crear la fila con `esEditable = 0`.
