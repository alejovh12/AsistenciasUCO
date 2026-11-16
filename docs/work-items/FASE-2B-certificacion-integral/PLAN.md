---
status: active
type: work-item
scope: backend
owner: backend-team
created: 2026-11-15
---

# PLAN — FASE 2B: Certificación integral de sesiones, tomas, asistencia, matrícula y reclamos

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-11-15 / Backend / rama fix/fase-2b-certificacion-integral; parte del commit F2A 01d60b0a, que parte del snapshot solicitado 9a72fce. F2A tiene PR #154 en borrador; develop remoto avanzó desde el snapshot y no se sincronizó esta rama.
- Frontend snapshot: develop 91f89f5681c560703f94e7feda0205e1f35f5513, solo lectura.
- DB snapshot: develop 39da95388eb4606991e99af94f060a898bf38593, solo lectura.
- Objetivo: certificar flujos reales de lectura por rol desde el navegador integrado y comprobar precondiciones operativas; no mutar la BD compartida. Ejecutar escrituras o cambiar comportamiento solo después de una enmienda específica, contrato aplicable y fixture controlado con limpieza segura.
- Criterios de aceptación: registrar PASS/FAIL/NOT_RUN con evidencia saneada por escenario; confirmar rol real manualmente; leer estado y ownership desde la aplicación real; cubrir puntos de control, TOM_012, minutos/estados, matrícula y reclamos cuando exista fixture seguro; no declarar certificación integral con escenarios críticos pendientes.
- Skills y fuentes autoritativas: uco-baseline, uco-seguridad, uco-testing, uco-contratos; AGENTS.md, SOURCE_OF_TRUTH, DEFINITION_OF_READY, API_DESIGN_RULES, BACKEND_GOLDEN_PATH_CONTRACT, OpenAPI Golden Path, HTTP_AS_IS_MATRIX, contratos y decisiones DB versionados por el owner y work items relacionados.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Las dos correcciones de Fase 2A están comprometidas | commit 01d60b0a; work item FASE-2A-altas-identity-drift | F2A-DEF-001 y F2A-DEF-002; PR #154 draft, sin merge. La validación live de altas sigue NOT_RUN. |
| La suite Backend actual pasa | VALIDATION.md de Fase 2A; Maven Java 25 | 4.539 tests, 0 fallos/errores/skips; JaCoCo LINE 94,92 %, BRANCH 86,53 %. No ejecuta integración SQL real ni navegador. |
| Respaldo SQL disponible y verificado | ruta local normalizada: %USERPROFILE%\.asistencias-security-lab\db-backups\gestionasistenciadb_antes_migrar_20261114-173254.bak | 66.203.648 bytes; RESTORE VERIFYONLY WITH CHECKSUM informó que el backup set es válido; cabecera msdb identifica gestionasistenciadb, backup completo COPY_ONLY con checksums, no dañado. SHA-256 coincide con la copia en el volumen SQL. No se restauró. Ver VALIDATION.md. |
| DB y runtime de Backend | .env local del Backend, consultado sin imprimir secretos | URL selecciona gestionasistenciadb; usuario runtime uco_dev_runtime, distinto de sa. El archivo .env no se modifica ni se versiona. |
| Workers con efectos | RotacionTokenAsistenciaScheduler, RotacionTokenMatriculaScheduler, CerrarTomasVencidas, CargaFestivosScheduler, EvaluacionAlertasInasistenciaScheduler, NotificacionOutboxDispatcher | Intervalos, precondiciones y overrides por proceso constan en VALIDATION.md. La lectura SQL encontró 0 tokens QR de asistencia vigentes, 0 ventanas de matrícula vigentes, 0 tomas abiertas y 19 festivos activos por año en 2026/2027 (horizonte configurado). No se deben abrir sesiones/tomas ni crear datos durante este recorrido. |
| SMTP está en sink local | configuración local del Backend y contenedor GreenMail | SMTP apunta al GreenMail local healthy. El dispatcher outbox se pausará explícitamente solo para este proceso, para no reservar/consumir/purgar filas preexistentes. |
| No hay fixture seguro para escrituras E2E | INT-SES-20261104, PC-puntos-control-tomas, helpers DB de tomas, análisis de Fase 2B | Grupos históricos 9105/9106 y seed 001 no tienen limpieza segura para E2E manual. No usar sus escrituras ni helpers destructivos en la BD compartida. |
| Sesiones y tiempo tienen diferencias contractuales | HTTP_AS_IS_MATRIX, OpenAPI v2 de sesiones, DEC-072, parser Backend, Frontend session-timezone.util, DB usp_validar_sesion_mismo_dia_interno | Backend/frontend razonan fecha local del cliente; DB aplica zona institucional. OpenAPI v2 no establece SES_028/regla de mismo día. No cambiar ni juzgar cruce de medianoche por suposición. |
| Cierre manual de sesión es ambiguo | HTTP_AS_IS_MATRIX y contrato DB SES_003 | La ruta existe, pero DB declara usp_cerrar_sesion no soportado y no persiste cierre; resolverlo requiere dictamen contractual. |
| Tomas DEC-066 requieren evidencia del owner | PC-puntos-control-tomas y VAL-oleada3-pru04 del repo DB | Se conoce el parámetro aprobado de máximo 20; no hay contrato DB completo aquí para la ventana [-15,+30) ni E2E 20/21. No generar una expectativa adicional. |
| Reclamos tienen bloqueos de contrato | HTTP_AS_IS_MATRIX, HU005-HU016, LB-004D, TECHNICAL_DEBT, DEC-073 ausente | Hay discrepancia de soporte requerido/opcional por categoría, documentos que mencionan una ruta retirada y evidencia ausente de DEC-073. Consultar solo AS-IS; bloquear expectativas dependientes. |
| Runtime Security Baseline local | imágenes Keycloak/PDP/PEP 0.1.11 | PDP y Keycloak healthy; PEP responde HTTP 200 en actuator/health y no tiene healthcheck Docker. README local menciona 0.1.13; no actualizar ni recrear contenedores. Confirmar login real antes de declarar autenticación PASS. |

## TARGET

Evidencia real y saneada del navegador integrado para las lecturas seguras por rol. Cada escritura queda NOT_RUN mientras no haya fixture aprobado, relaciones verificadas, cleanup reversible y protección contra notificaciones externas. Todo defecto Backend exige contrato AS-IS aprobado y enmienda con RED causal antes de tocar producción.

## Clase de cambio y alcance de rutas

- Change class: DOCUMENTATION_ONLY para el alcance actual.
- PRIMARY_VARIABLE: proporción de escenarios solicitados ejecutados de forma segura con resultado concluyente y evidencia; NOT_RUN se reporta aparte.
- ALLOWED_PATHS: docs/work-items/FASE-2B-certificacion-integral/**; docs/baseline/LINEA_BASE.md; docs/work-items/README.md; docs/baseline/MANUAL_VALIDATION_LEDGER.md; docs/work-items/FASE-2A-altas-identity-drift/VALIDATION.md (solo corregir el nombre del override de alertas en la evidencia de arranque heredada, sin alterar su resultado ni alcance).
- FORBIDDEN_PATHS: src/**, pom.xml, configuración persistente, scripts de runtime, Frontend, DB, SQL/SP/migraciones/permisos/semillas, providers externos, .env, worktrees, clones, contenedores o bases adicionales. Un defecto reproducible requiere una enmienda del PLAN y archivos permitidos explícitos antes de implementar.
- CONTRACTS: solo AS-IS de contratos versionados; no se modifica OpenAPI ni se crea API.
- PROVIDERS: SQL Server, Keycloak, PEP, PDP y GreenMail existentes; sin cambios de provider.
- EXTERNAL_ENVIRONMENT: copias de trabajo originales Backend/Frontend/DB; servicios existentes únicamente; pruebas funcionales desde navegador integrado.
- SECURITY_IMPACT: login manual por rol; no leer/copiar tokens, cookies, headers Authorization ni credenciales. Capturas/evidencias sin nombres, documentos, justificaciones, adjuntos ni PII real.
- OBSERVABILITY_IMPACT: registrar ruta, status, código funcional y correlation ID saneados; no almacenar raw logs.
- TEST_LEVEL_REQUIRED: navegador integrado contra Frontend real y Backend real para toda prueba declarada E2E; lecturas SQL solo para preflight y verificación de persistencia autorizada.
- ROLLBACK: detener procesos iniciados con su sesión de consola; no restaurar la BD, borrar filas, reiniciar/recrear contenedores ni limpiar datos existentes.
- CONSUMERS: Frontend snapshot 91f89f5; endpoints Backend según HTTP_AS_IS_MATRIX/OpenAPI; DB y Security Baseline como providers externos.
- STOP_CONDITIONS: candidata nueva de worker que pueda escribir; envío o consumo de notificación; fixture sin cleanup seguro; PII en pantalla/evidencia; cambio de usuario no confirmado; contrato contradictorio; respuesta inesperada con efecto de escritura.

## Alcance

- Preflight de respaldo, salud y workers; arranque controlado Backend con JDK 25 y launcher oficial.
- Lecturas visuales de grupos, sesiones por estado, tomas, asistencia/minutos, matrícula y reclamos disponibles para el rol autenticado.
- Confirmación visual de rol y ownership; request HTTP/status/código/correlation ID sin inspeccionar credenciales.
- Registrar límites, contratos conflictivos, resultado PASS/FAIL/NOT_RUN y pendientes por owner.
- Al solicitar login, pedir primero DOCENTE y esperar confirmación manual; cada cambio de rol también requiere login manual del usuario.

## No alcance

- POST/PATCH/PUT/DELETE contra la BD compartida; alta manual, creación/cierre de sesión/toma, asistencia manual/QR, matrícula o resolución/radicación de reclamo.
- Crear, restaurar o limpiar fixtures; ejecutar helpers destructivos; alterar el reloj del equipo.
- Adaptar Frontend, DB, contrato, RBAC, workers o proveedores a resultados de navegador.
- Implementar defectos sin nuevo DoR, contrato aprobado, TEST_PLAN, snapshot RED y auditoría.

## Archivos afectados

- EXISTENTES: docs/baseline/LINEA_BASE.md; docs/work-items/README.md; docs/baseline/MANUAL_VALIDATION_LEDGER.md.
- NUEVOS: docs/work-items/FASE-2B-certificacion-integral/PLAN.md, TEST_PLAN.md, VALIDATION.md; CLOSURE.md solo al cierre.
- RETIRAR: ninguno.

## Contratos y consumidores afectados

DOMAIN: DEC-066 para tomas y DEC-072 para horario, con evidencia del owner DB; conflictos pendientes.
HTTP: HTTP_AS_IS_MATRIX para operaciones no cubiertas; OpenAPI 1.7.0 para Golden Path y sesiones v2. No inventar respuesta de operaciones fuera de spec.
PERSISTENCE: DB develop 39da953; el Backend no administra esquema ni SP.
SECURITY: SecurityConfig, runtime-security-provider-architecture y contratos por endpoint; roles/ownership se verifican con sesión humana.
REALTIME: eventos son señal; HTTP/DB source of truth. No emitir comandos para provocar eventos.
Consumidor: Frontend develop 91f89f5, solo lectura; compatibilidad se observa, no se redefine.

## Riesgos y dependencias

- Usuario disponible para login manual de cada rol, sin compartir contraseña.
- Backend y Frontend locales compatibles con el Security Baseline existente.
- Los workers de tokens/tomas/festivos se comportan según preflight solo mientras no se creen sesiones/tomas/ventanas; una escritura nueva exige detener la prueba y evaluar de nuevo.
- Los overrides de alertas/outbox deben verse en el lanzamiento y quedar registrados; no editar .env.
- Nombres de variable por proceso: `OUTBOX_NOTIFICATION_DISPATCHER_ENABLED=false` y `APP_ALERTASINASISTENCIA_EVALUACION_ENABLED=false`. La propiedad `app.alertas-inasistencia.evaluacion.enabled` elimina guiones al formar la variable de entorno; se corrige la evidencia heredada de Fase 2A dentro de su VALIDATION.md para que el comando futuro no dependa de la forma incorrecta.
- Falta fixture E2E segura, decisión DB de ventana DEC-066, regla de cruce horario común, soporte de reclamo por categoría y DEC-073.

## Test plan

Ver TEST_PLAN.md. Secuencia: requisito/contrato AS-IS → behavioral matrix → preflight → lecturas en navegador → evidencia. No hay implementación ni RED Java mientras no se enmiende el alcance por defecto probado.

## Rollback y stop conditions

Desconectar los procesos Backend/Frontend arrancados mediante Ctrl+C en sus sesiones; confirmar puertos cerrados. No ejecutar restore ni cleanup de BD. Si un worker tiene candidatos o se observa una escritura no prevista, parar inmediatamente el Backend y registrar BLOCKED_BY_MISSING_EVIDENCE con consulta y responsable. CONTRACT_CONFLICT aplica a medianoche, cierre de sesión y reclamos; detener solo esos casos dependientes.

## Deuda conocida y validación manual

Registrar los IDs manuales existentes aplicables y agregar entrada MV solo cuando haya escenario real observado. TD-060, DEC-073, DR-LB004-DB-002, DEC-066 y DEC-072 permanecen conforme a la autoridad de su ámbito; esta fase no cambia deuda ni decisiones.

## Definition of Ready

NOT_READY para escrituras E2E, cierre funcional e implementación: no existe fixture con cleanup seguro y permanecen conflictos contractuales citados. DOCUMENTATION_ONLY de lecturas de interfaz queda autorizado por esta solicitud tras aplicar overrides explícitos de workers y confirmar el runtime. Responsable de contrato DB: owner DB; de DEC-073/DEC-072: owner contractual; de fixture: owner DB/QA. La investigación no convierte NOT_RUN en PASS.
