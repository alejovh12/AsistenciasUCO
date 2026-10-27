---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# VALIDATION — HU008: oferta de grupos del estudiante

| Capa | Comando | Resultado | Evidencia |
|---|---|---|---|
| DB | `freeze-manifest -Mode Verify` + `deploy_schema.ps1` + `test_summary.ps1` sobre SQL Server 2022 limpio | PASS | `HU008_OFERTA_*` (5), `HU008_RI01..RI03`, `S-01..S-12` (regresión de HU053-055); `TOTAL_EXECUTED=348`, `FAILED=0`, `DB GATE PASS` |
| Backend | `mvnw verify` (unitarias, ArchUnit, `AccessCatalogConsistencyTest`, `JpaRepositoryBeanUniquenessTest` con 48 repositorios, JaCoCo) | PASS | 2598 pruebas, 0 fallos, 0 errores, exit 0 |
| Backend | Cobertura de líneas de las clases nuevas (JaCoCo) | PASS | 94/94 = 100 % |
| Frontend | `npm run test:ci`, cobertura del código nuevo, `coverage:realtime:check`, `ng build --configuration=production` | PASS | 1267 SUCCESS; 96,8 %; PASSED; exit 0 |
| Documentos | WI en DB, backend y frontend; nota de la regla J4 en los WI de HU053-055; `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, índice de work items, MV-008, addendum de SP, contrato y mapa del frontend | PASS | ver [CLOSURE](CLOSURE.md) |

## Desviaciones

- **Sin commits RED separados:** las pruebas se escribieron junto con el código.
- **Fallo intermedio en la DB:** la primera corrida del gate falló por las tablas temporales `#r` y `#rx` que dejaba la prueba de regresión. Se corrigió (ver el VALIDATION de la DB).

## Pendiente

- GitHub Actions y SonarCloud: la rama está en commits locales, sin subir, hasta que John confirme.
- MV-008: oferta en pantalla y aceptar una transversal sin inscripción en el otro programa.
- Reaprovisionar el PDP con la ruta nueva (Alejo).
- Depuración de `EstudiantePrograma` antiguos: script manual listo. En la BD local de John el dry run dio 0 candidatas y no hubo borrado. En otros ambientes, dry run → respaldo → borrado.
