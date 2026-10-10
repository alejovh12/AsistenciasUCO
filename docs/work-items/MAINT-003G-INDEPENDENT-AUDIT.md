# MAINT-003G — Auditoría independiente UTC v2 (2026-10-10)

Estado: REVIEWED_WITH_BLOCKERS. Este documento no declara nuevos tests ejecutados ni autoriza merge/deploy.

## Fuentes verificadas
Backend `jose-valencia/maint-003f-utc-v2-implementation` (35 commits sobre develop), DB `feat/utc-d06-post-freeze` (1 commit sobre develop). Se confirmó en GitHub la publicación de ambas ramas y el acta `UTC_V2_CLOSURE_2026-10-10.md`. No hay PR UTC-D06 publicado en GitHub. El intento de escribir documentación en DB devolvió 403: permiso de integración insuficiente. No se alteraron datos ni se ejecutaron tests SQL/Maven en esta auditoría.

## Diagnóstico de gate
El acta local reporta Java25 unit 1492/1492 PASS, JaCoCo 88.80% line / 78.31% branch, SQL global 179 con 178 PASS/1 SKIP; **integración Java 191 tests con 11 failures/1 error/3 skips**, HTTP v2 con Keycloak real NOT_RUN. No afirmar READY; solicitar Surefire/Failsafe XML/log real para identificar nombres y causas. El acta agrupa: dos SP ausentes `usp_sincronizar_usuario` y `usp_registrar_o_actualizar_plan_estudio`, excepción hash, dos RAD/RES, seis fixtures asistencia y `APP_DATABASE_EXPECTED_NAME`. Revisar por test; no inventar SP ni cambiar contratos para verdes falsos.

## Hallazgos principales
1. **P1 seguridad/grants:** `schema/security/rol_asistencias_runtime.sql` concede `GRANT EXECUTE ON SCHEMA::dbo`; es demasiado amplio para mínimo privilegio. Inventariar SP usados realmente por JPA/runtime, construir GRANT objeto por objeto, preservar ownership de módulos y comprobar bajo **usuario SQL no privilegiado real**. El test `EXECUTE AS USER` existente es útil pero no sustituye una conexión con login de la aplicación. El backend reportó conexión con administrador del contenedor.
2. **P1 falso supuesto de protección trigger:** `tr_Sesion_procedencia_temporal.sql` verifica `UPDATE(procedenciaTemporal)`: detecta presencia en SET, no veracidad del valor. Un actor con DML arbitrario puede alterar horas y reafirmar marca anterior. Mantener restricciones SQL efectivas y auditar privilegiados; añadir test negativo `SET fechaHoraInicio=..., procedenciaTemporal=procedenciaTemporal` con rol runtime (rechazado por DENY). No atribuir al trigger capacidades que no tiene.
3. **P1 gate integración:** 11 fallos + 1 error impiden declarar BACKEND_READY. Aislar fixture/contrato y código real: registrar clase, método, excepción, SP esperado/presente, condición de datos, owner y arreglo mínimo. No reducir tests, @Disabled, skips ni aceptar mocks como SQL real.
4. **P1 startup compatibility:** `SesionV2SchemaCompatibilityVerifier` comprueba existencia de vista y 2 SP mediante `COL_LENGTH/OBJECT_ID`; no prueba versión, shape, EXEC efectivo, CHECK, permisos ni procedencia. Agregar smoke real con login runtime y pruebas para schema incompleto, una vez la BD migre.
5. **P2 work item DB:** la rama DB no contiene `docs/work-items/UTC-D06-POST-FREEZE/{PLAN,TEST_PLAN,VALIDATION}.md`. Preparar en worktree DB, guardar comandos y 179 results, migración incremental, fingerprints y rollback, 1 skip nombrado. Revisión owner obligatoria.
6. **P2 OpenAPI:** `OpenApiGoldenPathConformanceTest` registra 9 operaciones originales y 4 v2 y comprueba conjunto completo exacto (13); el ajuste es técnicamente válido. Mantener test dedicado de invariantes v1 y otro v2. Verificar checksum regenerado. Los históricos docs (MAINT-003D, PR20 y BACKEND_READY_FOR_FRONTEND) dicen "v2 no existe"; **son actas fechadas**, deben conservarse como histórico, pero agregar índice/nota que dirija al cierre 2026-10-10 y no confunda al lector.
7. **P2 semántica `DetalleAsistencia`:** SQL D06 focal verifica snapshot, y `usp_sincronizar_asistencia_estudiante_interno` copia horas al crear/actualizar detalle. Aclarar en contrato funcional que editar `Sesion` no reescribe detalles antiguos y que sincronización de asistencia sí refresca esas copias; testear ambos, evitando confundir snapshot permanente con mutable.
8. **P2 configuración de API v2 OFF:** `SesionesV2DisabledInterceptor` actúa después de filtro de seguridad HTTP. Con flag OFF, una petición anónima puede recibir 401 antes del 404 prometido; probar desde app real el orden, aclarar contrato deseado. No aflojar JWT.
9. **P2 publicación:** crear PR independientes backend y DB, no merge; CI remoto contra commit exacto, Sonar, CodeQL y Trivy; revisar alcance acumulado backend 35 commits y dependencias con PR18/19/20 para evitar cherry-pick/merge duplicado.

## Checklist de reparación para Codex
- Primero asegurar logs de integración, detallar 12 errores y 3 skips. Clasificar fixture vs divergencia de contrato vs bug. Arreglar en rama DB o backend correspondiente, conservando pruebas y baseline.
- Implementar pruebas unit faltantes: parser v2 (null, field unknown, timezone y rango), lectura id/grupo incluyendo NULL, puertos y ownership, JPA + SQL real, error 405, 401/403 v2, feature OFF y startup incompatible, serialización Z exacta.
- Ejecutar `./mvnw -B -ntp clean verify` y `./mvnw -B -ntp clean verify -Pintegration` en Java25; adjuntar Surefire/Failsafe, JaCoCo, 0 fallos y explicar skips.
- DB: hacer inventario y revisión de grants, D06 SQL, vistas v1 exactas, DST, concurrencia y read/write paridad con identidad no privilegiada; no desplegar principal ni clasificar históricos.
- Documentar work item DB versionado PLAN, TEST_PLAN, VALIDATION y acta backend actual, con SHAs, ejecución y decisiones de rollback.
- Con gates backend/DB en verde, hacer canario local HTTP POST→GET→PATCH→GET con Keycloak real y usuario SQL runtime. Sólo entonces publicar handoff READY_FOR_FRONTEND.
- Frontend puede empezar **en rama con flag OFF** usando contrato v2, pero nunca habilitar tráfico v2 productivo ni retirar v1 hasta pasar integración y consumidores.

## Trabajo realizado por esta auditoría
Se publicó solamente esta revisión documental en una rama aislada. Ningún archivo de producción ni test fue modificado; no se afirma reparación de los 11 fallos porque no se dispone del contenedor local ni de los XML/logs exactos. El permiso de GitHub del repo DB es read-only para cambios (403).
