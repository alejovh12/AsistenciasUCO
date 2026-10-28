---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# VALIDATION — HU094/HU095: ficha del docente y consolidado del directorio

| Capa | Comando | Resultado | Evidencia |
|---|---|---|---|
| DB | `freeze-manifest -Mode Verify` + `deploy_schema.ps1` + `test_summary.ps1` sobre SQL Server 2022 limpio (réplica del job DB Quality Gate) | PASS | `TEST_PASS:HU094_HISTORIAL_*` (6) y `TEST_PASS:HU095_RESUMEN_*` (3); `TOTAL_EXECUTED=349`, `FAILED=0`, `DB GATE PASS`, exit 0 |
| Backend | `mvnw verify` (unitarias, ArchUnit, `AccessCatalogConsistencyTest`, `JpaRepositoryBeanUniquenessTest` con 48 repositorios, JaCoCo) | PASS | 2578 pruebas, 0 fallos, 0 errores, exit 0 |
| Backend | Cobertura de líneas de las clases nuevas (JaCoCo) | PASS | 100 % en las clases nuevas. Las 7 líneas no cubiertas del conjunto medido (305) son preexistentes, de `DbFailureClassifier` y `DocenteProgramaWiringConfiguration`. Umbral de SonarCloud: 80 %. |
| Frontend | Karma (`npm run test:ci`) y `ng build --configuration=production` | ver el `VALIDATION.md` del work item homónimo del frontend | — |
| Documentos | WI en DB, backend y frontend; `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, índice de work items, MV-008, addendum de SP, contrato y mapa del frontend | PASS | ver [CLOSURE](CLOSURE.md) |

## Desviaciones

- Las pruebas se escribieron junto con el código en la misma sesión, sin commits RED separados.
- La ficha del frontend ya no muestra horario ni aula de los cursos del período (ver PLAN, riesgos).

## Pendiente

- GitHub Actions y SonarCloud: las ramas están en commits locales, sin subir, hasta que John confirme.
- MV-008: abrir la ficha de un docente y filtrar el directorio por tipo de vinculación con un coordinador real.
- Reaprovisionar el PDP con las 2 rutas nuevas (Alejo).
