---
status: active
type: validation
scope: fullstack-database-backend-frontend
last-reviewed: 2026-10-24
---

# VALIDATION — HU154 + HU023 + HU151 + HU152

## Estado por capa

| Capa | Tajada 1: tipos de programa | Tajada 2: áreas | Evidencia |
| :--- | :---: | :---: | :--- |
| DB: pruebas T-SQL | PASS (22-oct, BD local) | **NO EJECUTADAS** | Docker/SQL Server no estaba disponible el 24-oct. Tests RED y SP escritos y commiteados; falta correr `test_usp_*_area.sql`, `test_uv_area_contrato.sql` y la migración `06`. |
| Backend: `mvnw test` | PASS | PASS (suite completa: 2094 tests, 0 fallos) | Clases `*TipoPrograma*`, `*Area*`, `DbExceptionTranslatorTest`, ArchUnit. |
| Frontend: `ng test` / `ng build` | PASS | PASS (1064 tests, build OK) | `ng test --watch=false --browsers=ChromeHeadlessCI`. |
| E2E con JWT real | **Pendiente** | **Pendiente** | Requiere SQL Server, Keycloak y backend en puertos propios. |
| `infra.ps1 verify` | Pendiente | Pendiente | |
| Cobertura de código nuevo (Sonar ≥ 80 %) | Pendiente medir | Pendiente medir | Medir antes del PR. |

## Pendientes antes de certificar
1. Levantar SQL Server y ejecutar: migración `06_catalogos_area_estado_unicidad_up.sql`, SP de áreas, los 8 tests de catálogos y `test_suite.sql`.
2. E2E: ciclo crear → renombrar → inactivar → reactivar para tipo de programa y área; 403 con DOCENTE/COORDINADOR/DECANO.
3. Verificar que la migración `06` no encuentre duplicados en `Area` ni `TipoPrograma` de la BD real.
4. Deuda técnica registrada (D7): `usp_crear_asignatura` toma `TOP 1` área si no resuelve el nombre y no filtra por `estaActivaArea`.
