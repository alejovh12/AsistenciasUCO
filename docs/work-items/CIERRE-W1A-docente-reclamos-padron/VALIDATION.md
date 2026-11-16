---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# CIERRE-W1A (backend) - VALIDATION

Fecha: 2026-11-16. Rama `feat/cierre-w1a-docente-reclamos` desde `repo-antiguo/develop`. Contrato y plan: `CONTRACT.md` y `PLAN.md` de esta carpeta.
BD (PR previo): `johnjduque/gestion-asistencia-db` rama `feat/cierre-w1a-docente-reclamos` (los SP reciben los parametros nuevos).

## Cambios

| Archivo | Cambio |
| :--- | :--- |
| `DocentePortalController` | `GET /docente/reclamos` acepta `busqueda` (maximo 100, `BusquedaRequestValidator`) y `grupoId` (UUID) ademas de `estado`, `pagina`, `tamano` |
| `ConsultarReclamosDocente*` (DTO, mapper, dominio, caso de uso), `SolicitudRevisionDocenteQueryPort`, `SolicitudRevisionDocenteJpaRepository` | `busqueda` e `idGrupo` viajan hasta `usp_consultar_solicitudes_revision_docente`; el puerto conserva la sobrecarga de 4 argumentos (default) para consumidores anteriores |
| `docente/consultarpadrongrupo/**`, `PadronGrupoQueryPort`, `PadronGrupoPaginaProjection`, `GrupoJpaRepository`, `DocentePadronGrupoController`, `DocenteWiringConfiguration` | Ruta nueva `GET /docente/grupos/{grupoId}/estudiantes` (`ApiPageResponse<EstudianteGrupoDTO>`), mismo SP `usp_consultar_estudiantes_grupo_ambito_paginado` |
| `infra/security-baseline/access-catalog.json`, `docs/contracts/HTTP_AS_IS_MATRIX.md` | Alta de la ruta nueva (DOCENTE) y de los parametros nuevos |

`GET /api/v1/grupos/{grupoId}/estudiantes` (Golden Path, WORM) no cambia; `openapi-golden-path.yaml` y su `.sha256` no se tocan.

## Pruebas

| Prueba | Cubre |
| :--- | :--- |
| `ConsultarPadronGrupoFeatureTest` (nueva) | normalizacion, titularidad (403 sin docente o sin grupo), mapeo, total, valores por defecto |
| `DocentePadronGrupoControllerTest` (nueva) | `ApiPageResponse`, parametros, 400 de pagina/tamano/busqueda/UUID, limite de 100 caracteres |
| `GrupoJpaRepositoryPadronTest` (nueva) | parametros del SP, total de la 8.a columna, 403 `SEC_002`, filas con otra cantidad de columnas |
| `ConsultarReclamosDocenteFeatureTest` (ampliada y actualizada) | busqueda recortada y grupo hasta el puerto; titularidad con filtros. Se actualizaron 3 stubs/verificaciones a la firma de 6 argumentos del puerto (cambio de firma, misma intencion) |
| `DocentePortalControllerTest`, `SoporteArchivoJpaRepositoryTest` (ampliadas) | `busqueda`/`grupoId` en HTTP y en el SP; 400 por busqueda larga o UUID invalido |
| `AccessCatalogConsistencyTest` (existente) | la ruta nueva esta en el catalogo |

## Resultado del gate local

`.\mvnw.cmd clean verify` (Java 25, `-Djdk.net.unixdomain.tmpdir=C:\jtmp`): `Tests run: 4752, Failures: 0, Errors: 0, Skipped: 0`; `jacoco:check`: All coverage checks have been met
(bundle LINE 95,05 % / BRANCH 86,99 %; las clases nuevas al 100 % de lineas y ramas); `BUILD SUCCESS`.

Una primera corrida detecto una violacion real de ArchUnit (`CleanArchitectureRulesTest.domain_no_depende_de_output_ports`): el dominio del padron
referenciaba la proyeccion del puerto de salida; se corrigio en el codigo de produccion (`EstudiantePadronDomain` y mapper del caso de uso) sin tocar la regla.

No se corrio `-Pintegration` (sin BD desechable disponible a esa hora); queda al job de integracion de la CI. La paridad con los SP reales la cubre el gate de BD del PR hermano.
