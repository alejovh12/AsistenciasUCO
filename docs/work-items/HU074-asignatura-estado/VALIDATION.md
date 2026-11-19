# VALIDATION — HU074: Activar o inactivar asignaturas (backend)

> **2026-11-19 (integración):** la migración de HU074 se renumeró a la **58** y su semilla a la **70** al integrar NOTOMA-001 (Sergio, migración 57 / seed 69), que entró antes a develop.

## Dictamen

PASS local (unitarias, ArchUnit y JaCoCo) con la limitación de dependencias descrita abajo. Hubo revisión independiente
(2026-11-19); sus ajustes (cuerpo opcional `estaActiva`, estado del resultado del SP, estilo y pruebas) se aplicaron y se
volvió a correr el gate completo. E2E manual en pantalla HECHA el 2026-11-19 (abajo). La integración con la BD migrada
(`-Pintegration`, incluye la firma nueva en `SqlStoredProcedureContractIT`) la corre el CI.

## Confirmación de códigos contra la BD

Migración 58 del repositorio de BD (misma rama, sin commit), semilla `schema/seed/70_hu074_mensajes_estado_asignatura.sql` y
`docs/catalogos/catalogo-mensajes.md` (filas 117-120), leídas el 2026-11-19: `ASI_009`, `ASI_010`, `ASI_011` y `ASI_012` con los
mismos textos que usa este work item. `ASI_010` lo emite `usp_validar_asignatura_activa_interno` (llamado por `usp_crear_grupo` y
`usp_asociar_asignatura_plan`) con `usp_obtener_mensaje_catalogo`, es decir con el canal `DBCODE=ASI_010|…` que lee el
clasificador. Sin diferencias.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado |
|---|---|---|---|
| 2026-11-19, Windows 10, 8 GB, JDK 25, base `9adf99fb` + cambios sin commit | `mvnw.cmd -o -B -f <pom temporal> verify` | 0 | **BUILD SUCCESS. Tests run: 4891, Failures: 0, Errors: 0, Skipped: 0.** "All coverage checks have been met" (JaCoCo sobre 1939 clases: LINE 95,28 %, BRANCH 87,29 %; gates 80 % / 70 %). 41:28 min |
| 2026-11-19 | `py -3 -X utf8 <envoltorio de rutas largas> scripts/testing/matriz_hu_pruebas.py --write` | 0 | `MATRIZ_HU_PRUEBAS_BACKEND.md` regenerada: 72 HU con pruebas en 126 de 758 archivos; HU074 = 7 archivos, 55 casos |
| 2026-11-19, ajustes de la revisión, 1.ª corrida | `mvnw.cmd -o -B -f <pom temporal> verify` | 1 | 4913 pruebas, 1 fallo: `UseCasesShouldNotKnowSqlResultProtocolTest` (el `UseCaseImpl` leía `codigoResultado`). Corregido: el adaptador traduce el código del SP a `estaActivaResultante` y el caso de uso no conoce el protocolo |
| 2026-11-19, ajustes de la revisión, gate final | `mvnw.cmd -o -B -f <pom temporal> verify` | 0 | **BUILD SUCCESS. Tests run: 4914, Failures: 0, Errors: 0, Skipped: 0.** "All coverage checks have been met" (JaCoCo sobre 1941 clases: LINE 95,30 %, BRANCH 87,33 %). 18:38 min |
| 2026-11-19, tras el gate final | matriz HU (mismo envoltorio) | 0 | 72 HU con pruebas en 126 de 758 archivos; HU074 = 8 archivos, 65 casos |

Pruebas de este cambio en el gate final: `GestionarAsignaturaUseCaseImplTest` (25), `CoordinadorPortalControllerTest` (18),
`DbExceptionTranslatorAsignaturaEstadoTest` (15), `GrupoControllerWriteEndpointsTest` (11), `AsignaturaJpaCommandPatternTest` (8),
`AsignaturaPlanControllerComandosTest` (6), `CoordinadorWiringConfigurationTest` (1), `UseCasesShouldNotKnowSqlResultProtocolTest`
(1). Todas en verde.

Pruebas de la primera corrida (antes de los ajustes): `DbExceptionTranslatorAsignaturaEstadoTest` (11, nueva),
`DbFailureClassifierAsignaturaPlanTest` (10), `GestionarAsignaturaUseCaseImplTest` (14), `CoordinadorPortalControllerTest` (13),
`AsignaturaPlanControllerComandosTest` (6), `GrupoControllerWriteEndpointsTest` (11), `AsignaturaJpaCommandPatternTest` (5),
`MessageUcoLabCatalogoTest` (5), `MensajeUsuarioErrorCodeCatalogoTest` (4), `CoordinadorWiringConfigurationTest` (1). Todas en verde.

## Checks

| Nivel | Check | Estado | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | PASS | `PLAN.md`, `CONTRACT.md` |
| Arquitectura | ArchUnit/dependencias | PASS | incluidas en la corrida (`verify`) |
| Contratos | HTTP | PASS | 200 aditivo y 409 `ASI_006/009/010` fijados en pruebas MockMvc |
| Contratos | DB | PASS (estático) / CI | `usp_toggle_estado_asignatura` suma `@estaActivaDeseada bit` (último); `SqlStoredProcedureContractIT` actualizado, corre con `-Pintegration` |
| Duplicación | < 3 % | sin medir localmente | SonarCloud la mide en el PR; se unificó el `@CsvSource` repetido de `DbExceptionTranslatorAsignaturaEstadoTest` (`@MethodSource`) y el código nuevo no repite bloques |
| Tests | negativos | PASS | T04, T06, T09, T10, T12, T13 |
| Coverage | LINE ≥80 %, BRANCH ≥70 % | PASS | 95,30 % / 87,33 % (gate final) |
| Integración | DB real | NO EJECUTADA | la corre el CI (`-Pintegration`) con la BD de la misma rama |
| Build | mvn verify | PASS con limitación | pom temporal (abajo) |
| Seguridad | RBAC | sin cambio | |
| Documentos | matriz HTTP, matriz HU, LINEA_BASE | PASS | |

## Integridad de RED

NO APLICA: pruebas escritas junto con la implementación (sin tester independiente).

## Bloqueantes y limitaciones

- `messageucolab-spring-boot-starter:0.1.3` (GitHub Packages) sigue respondiendo 401 en este equipo; en `~/.m2` solo está la
  0.1.1. Igual que en `MEJORAS-SERGIO-2026-11-18`, se usó un pom temporal (sin versionar, borrado al terminar) con el starter en
  0.1.1 que excluye las 2 clases de INT-003 que necesitan la 0.1.3 (`MessageUcoLabServiceIdentityTokenProvider`,
  `MessageUcoLabSincronizacionConfiguration`) y sus 2 pruebas. Este cambio no toca esas clases. El `verify` con el pom real lo
  corre el CI.
- `matriz_hu_pruebas.py` falla en Windows con rutas de más de 260 caracteres; se ejecutó con un envoltorio (en el scratchpad, no
  versionado) que antepone `\\?\` a las rutas al leerlas. El script y su salida no cambian.

## E2E manual (hecha)

2026-11-19, en pantalla con `uco-coordinador`, BD con la migración 58 (antes del cuerpo opcional): inactivar una asignatura con
grupos vigentes → 409 `ASI_009`; inactivar sin grupos → 200 `ASI_012`; crear un grupo con la asignatura inactiva → 409
`ASI_010`; reactivar → 200 `ASI_011`. Asociar al plan una asignatura inactiva no se probó en pantalla (cubierto por MockMvc,
T13).

## Evidencias remotas/manuales pendientes

- CI del PR: `verify` con el pom real e integración con la BD migrada.
- E2E del cuerpo opcional `estaActiva` cuando el frontend lo envíe (y del despliegue conjunto con `@estaActivaDeseada`).
- Registrar `ASI_009`, `ASI_010`, `ASI_011` y `ASI_012` en MessageUcoLab y regenerar el fixture con `build-message-catalog.py`.
