---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# VAL-oleada9-backend - VALIDATION

Fecha: 2026-11-15 (noche). Worktree `fix/VAL-oleada9-backend` sobre `repo-antiguo/develop` 8530bd54. BD en
`fix/VAL-oleada9-mensaje-almacenamiento` (`johnjduque/gestion-asistencia-db`, develop 12e71eb). Windows 11, Java 25.

## REA-02 y H3 en el laboratorio

Se subió un PDF pequeño a `POST /api/v1/archivos/subir` con un estudiante de `datos-prueba` (`carlos.zapata@uco.edu.co`,
clave común de `AGENTS.md` solo en el proceso). Antes de cada caso se llamó hasta obtener 201 (el PEP dio un 503 en frío).
Cada contenedor se detuvo con `docker stop` unos segundos y se volvió a arrancar.

| Caso | Resultado |
| :--- | :--- |
| Control | 201 |
| `asis-antivirus-clamav` detenido (H3) | **503 `ERR_ANTIVIRUS_NO_DISPONIBLE`**, texto del catálogo: H3 confirmado en vivo |
| `asis-datos-minio` detenido | **500 `INTERNAL_ERROR`**: hueco de REA-02, corregido en esta oleada |
| Recuperación | 201 (después, 429 por el límite de subidas del estudiante, comportamiento esperado) |

Quedaron en el laboratorio unos pocos soportes de prueba en estado borrador del estudiante `carlos.zapata`.

**PEP caído (`e2e_baseline.py --pep-down`):** no se pudo medir. A las 04:13 UTC alguien recreó `asis-app-backend` con
`APP_ADAPTERS_SECURITY_PROVIDER=keycloak` (modo de contingencia, sin PEP); por eso las rutas no traen `X-Decision-Id` y
detener el PEP no cambia la respuesta. No se cambió esa configuración. El script ahora acepta `E2E_PEP_CONTAINER`
(`ext-seguridad-pep` en el stack por secciones) para repetir la medición cuando el laboratorio vuelva a `baseline`.

## RED → GREEN

| Prueba | RED | GREEN |
| :--- | :--- | :--- |
| `ArchivoAlmacenamientoNoDisponibleTest` (subir con MinIO caído, subir con el breaker `fileStorage` abierto) | `Status expected:<503> but was:<500>` (lo mismo que el laboratorio) | 2 de 2 |
| `MensajeUsuarioErrorCodeCatalogoTest` | `Codigos de error nuevos sin fila en el catalogo ... [ERR_ALMACENAMIENTO_NO_DISPONIBLE]` hasta sembrar la BD y regenerar el fixture | 4 de 4 |
| `MessageUcoLabCatalogoTest` | `expected: <482> but was: <486>` y `<122>` → `<123>`: el catálogo regenerado trae `ERR_ALMACENAMIENTO_NO_DISPONIBLE` y además `VCE_009`, `ACT_001` y `ACT_002` (IDENT-002, ya en los seeds de develop pero no en el fixture) | 5 de 5 con los conteos nuevos y su origen en el comentario |
| `test_e2e_baseline_usuarios.py` | — | 5 pruebas (2 nuevas de `contenedor_pep`); la suite de scripts pasa |

**Corrección:** `SubirArchivoUseCaseImpl` traduce `FileStoragePort.StorageUnavailableException` al almacenar en
`InternalApplicationException.servicioNoDisponible(ERR_ALMACENAMIENTO_NO_DISPONIBLE)` → 503, igual que el antivirus.
Primero se intentó en `GlobalExceptionHandler` para cubrir también la descarga, pero `ControllersMustDependOnlyOnInputPortsTest`
falló (la capa de controladores no puede depender de `FileStoragePort`): se movió al caso de uso, sin tocar la regla. La
descarga sigue en 500 porque `DescargarArchivoUseCaseImplTest.storage_no_disponible_no_se_disfraza_de_archivo_inexistente` fija
que el caso de uso propague la excepción; cambiarla es una decisión del dueño (conflicto prueba-contrato).

## Catálogo y BD

- **Fixture:** `catalogo-mensajes-usuario.psv` regenerado con
  `build-message-catalog.py --seed <BD>/schema/seed --previo <fixture> --psv`: 486 mensajes, 123 alias;
  `ERR_ALMACENAMIENTO_NO_DISPONIBLE` → `ERR121` (alias agregado a `application.yml`).
- **BD (rama aparte):** fila en la semilla 63, migración `47_val_mensaje_almacenamiento` (up y down), marcador en
  `scripts/migrar-marcadores.json`, prueba `test/test_val_oleada9_mensaje_almacenamiento.sql`, manifiesto `freeze-manifest.ps1`
  actualizado (`FREEZE_MANIFEST_OK`) y fila en `docs/catalogos/catalogo-mensajes.md`.
- **Laboratorio:** la prueba SQL falló antes de la 47 y pasó después; up dos veces (idempotente), down (la prueba vuelve a
  fallar) y up final.

## Incidente en la BD del laboratorio (reparado)

Al aplicar la migración 47 se usó `scripts/migrar.ps1 -Modo Aplicar`. La base del laboratorio se recreó con
`deploy_schema.ps1` y su `MigracionAplicada` estaba vacía, así que el script re-ejecutó las migraciones 05 a 12 y falló en la 13.
Efectos encontrados y revertidos el mismo día:

- `uv_categoria_justificacion` quedó en la versión de la migración 12, sin `requiereSoporte`: restaurada desde `schema/views`
  (6 categorías, con la columna).
- La 13 creó 4 tablas `zz_archivo_*` (3 plantillas y 6 marcadores archivados): eliminadas.
- 13 filas nuevas en `MigracionAplicada`: eliminadas (vuelve a 0, como estaba).
- Sin efecto neto: los objetos que esas migraciones borran no existen en el esquema vigente; los `ALTER TABLE` tienen guarda;
  los estados de notificación y la plantilla `RETIRO_ESTUDIANTE_GRUPO` quedaron iguales a la semilla vigente; las plantillas
  `SOLICITUD_INGRESO_*` que reinsertó la 08 las volvió a retirar la 13 (0 filas).

La migración 47 se aplicó después directamente con `sqlcmd -i`, sin registrarla.

## MSG-05: recuento de textos quemados en Application

Recuento sobre `src/main/java/.../application` con la misma expresión de `MensajeUsuarioErrorCodeCatalogoTest`
(`new ValidationException|ForbiddenException|ResourceNotFoundException|ConflictException|FeatureUnavailableException|
BusinessException|TooManyRequestsException("texto"` sin código):

| Medida | Antes (lista del 2026-11-13) | Ahora |
| :--- | :--- | :--- |
| Excepciones de negocio con texto y sin código de catálogo | 231 | **0** (la prueba falla si aparece una) |
| Códigos `ERR_*` sin fila en el catálogo (`codigos-error-sin-catalogo.txt`) | 93 | **0** |
| Otros literales `new X("texto")` | — | 136: 93 `CrosscuttingException` (guardas de precondición de programación), 38 `InternalApplicationException` y 5 de otros tipos técnicos |

Los textos técnicos no llegan al usuario: `InternalApplicationException(String)` usa la definición `INTERNAL_APPLICATION_ERROR`
y la respuesta lleva su mensaje seguro de catálogo (`GlobalExceptionHandlerTest.handleInternalApplication_retorna_500_sin_mensaje_tecnico`).
`VALIDATION_ERROR` se conserva: es el código del contrato para el 400 con `details[]`.

## PRU-09: matriz HU → pruebas

| Comando | Resultado |
| :--- | :--- |
| `python -X utf8 -I scripts/testing/matriz_hu_pruebas.py` sin la matriz | `MATRIZ_HU_PRUEBAS_DESACTUALIZADA`, exit 1 |
| `... --write` | Escribe `docs/testing/MATRIZ_HU_PRUEBAS_BACKEND.md` |
| `...` (compuerta) | `MATRIZ_HU_PRUEBAS_PASS Resumen: 71 HU con pruebas en 125 de 737 archivos de prueba.`, exit 0 |

El paso "HU test matrix is up to date" corre antes de Maven en `backend-ci.yml`.

## Compuerta del backend

| Comando | Resultado |
| :--- | :--- |
| `./mvnw.cmd -B -ntp clean verify` (primera corrida) | Falló `ControllersMustDependOnlyOnInputPortsTest`: el manejador en `GlobalExceptionHandler` dependía de `FileStoragePort`. Se movió la traducción al caso de uso |
| `./mvnw.cmd -B -ntp clean verify` (final) | `BUILD SUCCESS`, 12:12 min; 4715 pruebas, 0 fallos, 0 errores, 0 omitidas; JaCoCo cumplido (líneas 95,03 %, ramas 86,90 %) |

## Pendiente

- Backend Integration Gate en el PR.
- Gate de BD completo (deploy limpio + `test_summary.ps1`) en el CI del PR de BD.
