---
status: active
type: work-item
scope: backend-db-frontend
owner: backend-team
last-reviewed: 2026-11-19
---

# VALIDATION — SES-GEN-01 (2026-11-19)

Contrato implementado: [CONTRACT.md](CONTRACT.md). Sin laboratorio, base desechable ni respaldos (orden del responsable): todo sobre los repos oficiales y la base de desarrollo `sql_server_asistencias` (1433). Ramas locales `feature/ses-gen-01-calendario-semestre` en los tres repos; **sin commit ni push**.

| Capa | Comando / acción | Resultado |
|---|---|---|
| DB | Migración 57 (up) y objetos aplicados a la base de desarrollo con `sqlcmd` (antes, los 12 SP/vistas tocados eran idénticos al repo `develop @ dadf8dc`) | OK |
| DB | `test/test_ses_gen_01_calendario_semestre.sql` (transacción con ROLLBACK, 9 casos: período calculado/rechazado/edición, B1/B2, B5, B7, B2b 16 y 32 sesiones con `numeroSemana` 1..16 en período que empieza en miércoles, previsualización = precarga, `ASI_009`) | PASS 9/9 |
| DB | `test_usp_periodo_academico_gestion.sql` (ajustado a períodos de 112 días), `test_usp_crear_actualizar_grupo.sql`, `test_culm001_b2nm_asignatura_plan.sql`, `test_rec2_rbac_asignatura.sql`, `test_subject_catalog.sql` | PASS |
| DB | `test_usp_guardar_horarios_grupo.sql`, `test_usp_precargar_sesiones_grupo.sql`, `test_cruce_horario_docente.sql`, `test_dec073_validaciones.sql` (último caso) | **NOT_RUN efectivo**: fallan en el fixture por `DocentePrograma.tipoVinculacion` (la base de desarrollo no tiene aplicada la migración 56, ajena a este cambio). Sus franjas (60–240 min, 06–22 h) no chocan con B7 y sus materias no tienen horas (B5 no aplica) |
| DB | `test_summary.ps1` completo | **NOT_RUN** (incluye pruebas up/down destructivas que no se ejecutan contra la instancia de desarrollo) |
| Backend | `mvnw test` (unitarias), 4911 pruebas | 4910 PASS + 1 ajuste de conteo del catálogo corregido y revalidado (`MessageUcoLabCatalogoTest`, `MensajeUsuarioErrorCodeCatalogoTest` PASS). La corrida completa posterior al ajuste no se repitió entera |
| Backend | `mvnw package -DskipTests` y arranque con `start-backend-baseline.ps1` (modo baseline, PEP/PDP reales) | OK |
| Infra | `provision_asistencias.py catalog` (198 endpoints, 0 sin regla); `pdp` (recurso nuevo aprovisionado); `verify`: `missing_grants` vacío, deriva `extra_grants` preexistente (no se usó `--prune`) | OK |
| E2E HTTP | Token real de `uco-coordinador` contra el backend: `GET /coordinador/asignaturas` (22 columnas, `horasSemanales` nulo), previsualización 200 con 39 sesiones del período histórico, conflictos `HOR_002`/`HOR_013`/`HOR_012` como datos, 400 sin franjas, `POST /grupos` sin franjas 400 `ERR_CAMPO_OBLIGATORIO`; `uco-administrador`: `PUT /grupos/{id}` 403 y previsualización 403 | OK |
| Frontend | `tsc` app y specs, `eslint` de los archivos tocados, `lint:tokens`, `lint:a11y`, `ng build` (AOT) | OK |
| Frontend | `ng test` completo (ChromeHeadless) | 3570 PASS, 0 fallos |

## No verificado (NOT_RUN)

- Recorrido manual en navegador (MV): login del coordinador, crear materia con horas, crear período, crear grupo con previsualización, editar horario.
- Creación real de un grupo, período o materia de punta a punta por HTTP (las escrituras se probaron en la DB con ROLLBACK y en pruebas unitarias; el E2E HTTP fue de lectura y de rechazos).
- `usp_previsualizar_horario_grupo` con grupo existente y con estudiantes inscritos (HOR_009), y festivos cargados en el rango (los casos de festivo se cubren por lectura del SP y el ejemplo de la previsualización del período vigente).
- Regresión de la suite DB completa (ver arriba).

## Datos de la base de desarrollo

Las materias existentes tienen `horasSemanales` NULL (sin carga inicial, decisión 1c): hasta que el coordinador las complete, el horario se calcula pero B5 no bloquea y se avisa con `HOR_013`. El único período (`2026-2`, 137 días) es histórico y se conserva.
