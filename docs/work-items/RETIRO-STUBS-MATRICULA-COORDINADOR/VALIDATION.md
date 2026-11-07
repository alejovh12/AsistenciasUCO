# VALIDATION — RETIRO-STUBS-MATRICULA-COORDINADOR

| Comando | Resultado |
|---|---|
| `./mvnw verify` (2026-11-07, Java 25) | `Tests run: 3310, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS` (incluye ArchUnit, JaCoCo y `AccessCatalogConsistencyTest`) |
| `CoordinadorPortalControllerTest` | `stubs_de_matricula_y_solicitudes_del_coordinador_retirados_no_tienen_ruta` pasa: el controlador ya no mapea `/solicitudes-matricula` ni `/grupos/{grupoId}/estudiantes` |
| `access-catalog.json` | JSON válido; 4 rutas menos |

Sin validación manual: no cambia ninguna pantalla. Queda pendiente el `--prune` del PDP (solo con aprobación de John).
