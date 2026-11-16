# VALIDATION — FASE 2B: certificación integral

## Dictamen

IN_PROGRESS. La suite Backend y el respaldo están PASS; el preflight y el arranque local controlado terminaron sin cambios observados en la BD. El navegador integrado quedó disponible el 15/11 (sesión posterior); se completó el recorrido autenticado manual de los cinco roles (ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE y ESTUDIANTE), documentado en sus secciones respectivas más abajo y resumido en "Cierre consolidado de la certificación funcional (15/11/2026)". No se declara la certificación integral: quedan hallazgos P1 abiertos (incluido uno que bloquea por completo un flujo normativo de DOCENTE) y partes del alcance de escritura (matrícula, asistencia manual/QR, cierre masivo, cambios de período activo) permanecen NOT_RUN por diseño de este recorrido.

## Recorrido ADMINISTRADOR (15/11/2026, navegador integrado, login manual del usuario)

Backend/Frontend/Security Baseline arrancados con los lanzadores oficiales (overrides `OUTBOX_NOTIFICATION_DISPATCHER_ENABLED=false` y `APP_ALERTASINASISTENCIA_EVALUACION_ENABLED=false` activos). Microfrontends `security-ui` (5175) y `securityBaseline-vue` (5173) estaban caídos al abrir "Seguridad UCO" (`ERR_CONNECTION_REFUSED`); se levantaron con `infra/Frontend_Seguridad/scripts/start-frontends.ps1` sin tocar Docker. Rol real confirmado vía `GET /api/v1/auth/me` → `ADMINISTRADOR`.

Pantallas recorridas con pruebas de formulario, filtros, paginación y persistencia: Panel Principal, Estructura > Facultades e Instituciones, Personas > Estudiantes/Docentes/Decanos/Asignaturas, Configuración (Parámetros, Catálogos, Estados, Festivos, Correo y notificaciones, Áreas, Períodos), Sistema > Trazabilidad y Auditoría / Cierre Masivo de Asistencia, Indicadores, Seguridad UCO, Mensajes institucionales, Mi Perfil.

Datos de prueba E2E creados (sintéticos, identificables, reversibles): facultades `Facultad E2E Cert 15112026` (activa) y `Facultad E2E Cert2 15112026` (desactivada tras la prueba); estudiante `900555001` / `e2e.cert.20261115@uco.edu.co` (admitido, sin matricular).

### Hallazgos — ADMINISTRADOR

| # | Severidad | Responsable probable | Pantalla/Acción | Resumen | Evidencia |
|---|---|---|---|---|---|
| F-ADM-01 | P1/P2 | FRONTEND + BACKEND/DB (pendiente dictamen owner) | Estructura > Facultades > Nueva Facultad | El `<select>` de "Decano(a) Titular Asignado" nunca queda en el placeholder: al abrir el formulario toma por defecto el primer decano de la lista sin marcarlo como pendiente de elegir. Al crear una facultad llenando solo el nombre (sin tocar el combo), el backend aceptó `POST /admin/facultades` → 201 asignando como titular a un decano que YA era titular de otra facultad (sin 409 de conflicto). Reproducido dos veces. | correlationId `80d0c537-1c10-41fa-b30d-a147dc360123` (registrado en Trazabilidad y Auditoría) |
| F-ADM-02 | P2 | FRONTEND + BACKEND (contrato) | Personas > Estudiantes > Admitir estudiante | El bloque "+ Asignar contraseña inicial explícita" se presenta como opcional/colapsado, pero el backend rechaza la admisión sin contraseña (`400 ERR_PASSWORD_REQUERIDO`). El FE recupera bien (revela el campo tras el error), pero el rotulado induce a un primer intento fallido. | correlationId `c1b05cea-08da-4d19-af7b-3d0bbbc373ba` |
| F-ADM-03 | P2 | FRONTEND | Personas > Docentes | El filtro "Programa" queda siempre vacío para ADMINISTRADOR porque la pantalla llama `GET /api/v1/decano/programas` (ámbito DECANO) → 403, en vez de un endpoint de catálogo accesible para administración. | Network: `GET /api/v1/decano/programas?estaActivo=true&pagina=1&tamano=100` → 403 |
| F-ADM-04 | P2 | FRONTEND | Personas > Decanos > Nombrar Decano Existente > Vincular | Al vincular un docente YA existente (con cuenta institucional activa) como decano, el formulario "Registrar Nuevo Decano" prefija nombre/documento/correo correctamente pero sigue exigiendo "Contraseña inicial de acceso" como obligatoria, pese a que la persona ya tiene credenciales. Bloquea el guardado mientras no se invente una contraseña nueva para alguien que no la necesita. No se completó el guardado para no alterar la cuenta real de la docente seed. | Botón "Guardar Decano" con `disabled=true` hasta rellenar contraseña; JS: `value` de `correo`/`numeroIdentificacion` correctamente prefijados |
| F-ADM-05 (menor) | P3 | FRONTEND | Personas > Decanos > Vincular docente | El mapeo de "Maria Elena Rostagno Valencia" a los campos separados del formulario de decano reparte mal: `primerNombre="Maria"`, `primerApellido="Elena Rostagno Valencia"` (no separa segundo nombre/apellidos). | JS: lectura de `value` de los inputs del formulario |
| F-ADM-06 (menor) | P3 | FRONTEND | Personas > Decanos > Corregir datos | A diferencia de "Nueva Facultad"/"Admitir estudiante", este modal no muestra mensaje de error inline para un nombre inválido ("Juan123"); solo se infiere por el botón "Guardar corrección" deshabilitado, sin feedback visible al usuario. | DOM: `disabled=true` confirmado por JS, sin texto de error visible |

### NOT_RUN deliberados (por las reglas de seguridad del recorrido, no por imposibilidad técnica)

- "Ejecutar cierre definitivo" (Sistema > Cierre Masivo de Asistencia): acción real e irreversible sobre el período institucional vigente.
- "Eliminar"/"Editar Fechas" del período 2026-2 (único período activo de la institución).
- "Recargar año" en Festivos (podría disparar `CargaFestivosScheduler`).
- "Definir rol"/"Editar"/"Agregar administrador"/"Retirar" en Seguridad UCO (cambios estructurales de seguridad compartida).
- "Enviar Correo de Prueba en Vivo" (Correo y notificaciones): bloqueado por la política de envío de comunicaciones de la sesión del agente, no por la aplicación.
- Asignación del rol ESTUDIANTE al estudiante de prueba vía "Asignar acceso": no aparece en el directorio del PDP porque, según el código (`AccesoCuentaService.reconciliar`, invocado desde `RegistrarEstudianteUseCaseImpl`), el aprovisionamiento de perfil se difiere hasta la matrícula en grupo — fuera de alcance de ADMINISTRADOR y de F2B.

### Dependencias externas confirmadas no disponibles en esta instalación (no son defectos de AsistenciasUCO)

- Catálogo de Parámetros (Configuración > Parámetros / Sistema > Parámetros): "no disponible por ahora".
- MessageUcoLab (Mensajes institucionales): `502` en `/messageucolab/v1/session/applications`, con manejo de error correcto; este mismo polling explica los `502` vistos en consola en casi todas las pantallas del recorrido (no son errores nuevos por pantalla).

### Checks que PASARON sin hallazgos

Dashboard/KPIs, 404 de ruta inexistente con sesión persistente, Facultades (crear/editar/desactivar con datos válidos), Instituciones (solo lectura), Estudiantes (listado 5007, búsqueda con/sin resultado, filtros, paginación, ficha con historial), validaciones de nombre/documento/correo en Admitir estudiante (letras en documento, números en nombre, tildes/ñ, correo sin @, correo no institucional — las 5 con mensajes correctos y sin POST), Decanos (búsqueda en vivo, Corregir datos con prefill correcto y sin pedir contraseña/correo), Asignaturas (catálogo solo lectura), Catálogos/Estados/Áreas (solo lectura), Festivos (19 activos 2026/2027, sesión en festivo con asistencia consistente con anomalía ya documentada), Outbox (1121 pendientes, 0 reservadas, dispatcher confirmado pausado), Canal SMTP (diagnóstico honesto "Inactivo"), Trazabilidad y Auditoría (2501 registros, correlaciona exactamente las acciones reales ejecutadas, sin exponer secretos), Indicadores (KPIs y gráficas con datos reales), Seguridad UCO (Roles/Perfiles/Recursos/Administradores visibles), Mi Perfil.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-11-15, Windows/JDK 25, 01d60b0a | .\mvnw.cmd verify | 0 | 4.539 tests; 0 fallos/errores/skips. LINE 94,92 % (22.416/23.616); BRANCH 86,53 % (7.019/8.112). | Suite/ArchUnit/JaCoCo locales; no incluye -Pintegration ni E2E. |
| 2026-11-15, SQL Server existente | RESTORE VERIFYONLY FROM DISK = N'/var/opt/mssql/backup/gestionasistenciadb_antes_migrar_20261114-173254.bak' WITH CHECKSUM | 0 | "The backup set on file 1 is valid." | DB metadata: gestionasistenciadb, D, COPY_ONLY=1, checksum=1, is_damaged=0; host y volumen SHA-256 iguales. No restore. |
| 2026-11-15, SQL Server existente | SELECT de vistas de token, tomas abiertas, festivos y parámetros | 0 | 0 tokens/tomas elegibles; 19 festivos activos en 2026 y 2027. | Solo conteos/flags; sin IDs personales ni cambios. |
| 2026-11-15 23:29:20 UTC, SQL Server existente | SELECT agregado de `uv_token_asistencia_toma`, `uv_token_matricula_grupo`, `TomaAsistencia`/`SesionCancelacion`, `uv_dia_festivo` y parámetros de jobs | 0 | QR y matrícula habilitados; 0 tokens activos/por rotar, 0 ventanas vigentes/por rotar, 0 tomas abiertas/candidatas a cierre; festivos habilitado con horizonte 1 y 19 activos por año en 2026/2027. | La consulta inicial con `uco_dev_runtime` devolvió permiso denegado para `ufn_obtener_parametro_int` y `SesionCancelacion`; sin cambios. Se repitió como SELECT agregado mediante la cuenta administrativa ya presente en el contenedor existente. No se muestran IDs ni PII. |
| 2026-11-15 23:32:25 UTC, antes de que Backend escuchara en 8080 | SELECT de elegibles según los predicados de rotación, cierre, alertas y reserva de outbox; festivos activos por año | 0 | Candidatos a rotación/cierre/evaluación de alertas=0; festivos activos 2026/2027=19/19; 1.158 mensajes reservables; 0 terminales elegibles para depuración por antigüedad. | Snapshot justo antes de que el servicio estuviera disponible; solo SELECT, sin IDs/PII. Fuentes de predicados: SP de DB para rotación/cierre/alertas y repositorio del dispatcher para outbox. |
| 2026-11-15, antes del arranque | SELECT agregado `NotificacionSaliente` por `EstadoNotificacion.codigo` | 0 | ENTREGADA=506, FALLIDA_DEFINITIVA=6, FALLIDA_REINTENTABLE=38, PENDIENTE=1.120. | No se leyeron contenidos ni destinatarios; se repitió el conteo después del arranque para confirmar que el override evita consumo/depuración. |
| 2026-11-15, Security Baseline local | Docker inspect + GET PEP /actuator/health | 0 | Keycloak/PDP healthy; PEP HTTP 200, sin healthcheck Docker. GreenMail healthy. | Imágenes 0.1.11; README local menciona 0.1.13, sin actualización. |
| 2026-11-15 18:32 (-05:00), Backend | `infra/security-baseline/scripts/start-backend-baseline.ps1 -Provider baseline -Port 8080 -BackendEnv %LOCALAPPDATA%\AsistenciasUCO\backend-run\phase2b-safe-workers.env` | proceso activo hasta 18:37 | JDK 25; HTTP 8080, management 8090; `/actuator/health` status `UP`; perfil local; verificador de contrato DB UTC-D06-POST-FREEZE PASS. Log explícito: `dispatcherEnabled=false`. | El archivo de override solo contiene dos booleanos false; `.env` no se modificó. `ddl-auto: none`, `spring.sql.init.mode: never`; no se aplicaron migraciones. SMTP test mode redirige a un buzón de prueba sin registrar destinatario. El aviso de FilterRegistrationBean PEP deshabilitado es intencional: el filtro solo se añade a `SecurityFilterChain` (`SecurityBaselinePepConfiguration`/`SecurityConfig`). |
| 2026-11-15 18:33 (-05:00), Frontend original | `node_modules/.bin/ng.cmd serve --host 127.0.0.1 --port 4200` | proceso activo | Angular real disponible solo en loopback, puerto 4200. La configuración fuente usa API real (`useMocks` false por defecto). | Build dev mostró avisos NG8113 por imports no usados en componentes; sin errores de compilación ni cambios al Frontend. Pendiente inspección visual desde navegador integrado. |
| 2026-11-15 18:34 (-05:00), SQL Server existente | SELECT agregado de estado de `NotificacionSaliente` antes y después de iniciar Backend | 0 | Conteos idénticos: ENTREGADA=506, FALLIDA_DEFINITIVA=6, FALLIDA_REINTENTABLE=38, PENDIENTE=1.120. | Confirma que no se consumió ni depuró el outbox durante el arranque; dispatcher apagado explícitamente. Solo conteos, sin datos de mensajes/destinatarios. |
| 2026-11-15 18:37 (-05:00), Backend/Frontend | Ctrl+C en las sesiones del launcher oficial y Angular CLI | 0 | Ambos servidores cerraron ordenadamente; puertos 8080/8090/4200 comprobados libres tras el cierre. | No se tocaron contenedores ni se hizo cleanup de datos. |
| 2026-11-15, Backend | git diff --check antes de commit F2A | 0 | PASS | commit F2A 01d60b0a; archivos locales protegidos fuera de commit. |
| 2026-11-15, copia original Frontend/DB | `git rev-parse --short HEAD`; inspección de estado | 0 | Frontend `91f89f5`, DB `39da953`; solo lectura. | DB conserva sus modificaciones locales de `.env.example` y `.env.template`; no se tocaron. Frontend sin cambios locales. |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | PLAN/criterios | PASS para investigación documental y preflight; NOT_READY para mutaciones | No hay fixture seguro ni contrato completo para escenarios críticos. |
| Arquitectura | ArchUnit/dependencias | PASS | Incluido en verify local del commit 01d60b0a. |
| Contratos | HTTP/DB/security/eventos | BLOQUEADO parcial | Discrepancias de cruce de medianoche, cierre manual, DEC-066, DEC-073/soporte/plazo; no se cambian por suposición. |
| Tests | RED → GREEN, negativos | PASS para correcciones F2A; NO APLICA a F2B documental | F2A RED/GREEN y hashes preservados; esta fase aún sin implementación. |
| Coverage | Gates | PASS | LINE 94,92 %; BRANCH 86,53 %. |
| Integración | DB real, fixtures, skips | BLOQUEADO para escrituras | Solo consultas SQL de preflight; fixture manual seguro no identificado; -Pintegration NOT_RUN. |
| Build | mvn verify | PASS | Exit 0, 4.539 tests, 0 skips. |
| Seguridad | Secretos, RBAC, scanners | PARCIAL | No se imprimieron secretos ni se consultaron tokens; autenticación/roles en navegador pendientes. |
| Documentos | Enlaces, deuda, manuales, ADR | EN CURSO | PLAN/TEST_PLAN/VALIDATION creados; ledger manual y cierre se actualizan tras observar navegador. |
| Navegador | E2E integrado | PASS para los 5 roles | Sesión posterior (15/11): navegador integrado disponible; login manual real de ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE y ESTUDIANTE, rol confirmado en cada caso por `/api/v1/auth/me`. Recorrido completo documentado en las secciones "Recorrido <ROL>" y resumido en "Cierre consolidado de la certificación funcional". |

## Integridad de RED

F2B no tiene RED Java mientras siga DOCUMENTATION_ONLY. Para F2A, snapshots RED originales están documentados en su TEST_PLAN.md; commit 01d60b0a conserva los tests y GREEN. No se cambiaron los tests RED al implementar.

## Bloqueantes y limitaciones

- No se reutilizarán grupos persistidos 9105/9106 ni seed 001 para escrituras: son datos históricos y no tienen cleanup seguro para E2E manual.
- Los límites DEC-066 20/21 y [-15,+30), TOM_012 y asistencia QR/manual requieren fixture controlado y, donde falta, contrato DB vigente.
- Reclamos: falta DEC-073, hay discrepancia entre soporte opcional y requerido por categoría y dudas sobre plazo/archivos; se informará al owner en evidencia, sin ampliar contrato.
- Sesiones: regla de medianoche cliente vs zona institucional y cierre manual SES_003 siguen CONTRACT_CONFLICT.
- No se ejecutó -Pintegration verify, no se abrió navegador, no hubo login ni requests funcionales.

## Recorrido DECANO (15/11/2026, navegador integrado, login manual del usuario)

Login manual como `uco-decano` (Juan Carlos Gomez Perez, decano titular de Facultad de Ingeniería). Rol real confirmado vía `GET /api/v1/auth/me` → `DECANO`. Pantallas recorridas: Panel Principal, Mi Facultad (Programas/Coordinadores/Áreas, incluido "Nombrar Coordinador Existente"), Directorio (Docentes/Estudiantes/Asignaturas/Grupos), Períodos Académicos, Seguimiento (Indicadores/Consolidado/Cronograma), Revisiones de Cancelación (consulta). No se ejecutaron escrituras reales (se canceló todo intento de "Vincular" antes de guardar, para no alterar cuentas reales de docentes seed).

### Hallazgos — DECANO

| # | Severidad | Responsable probable | Pantalla/Acción | Resumen | Evidencia |
|---|---|---|---|---|---|
| F-DEC-01 | P2 | FRONTEND o BACKEND (dos criterios de "vigente" distintos) | Panel Principal (dashboard) | El KPI "Grupos en Curso" muestra "— / Sin período vigente", pero la pantalla dedicada "Períodos Académicos" y "Mi Facultad > Programas" (mismo rol, mismo período 2026-2, 2026-08-01 a 2026-12-15, hoy 15/11/2026) muestran "● Período Académico en Curso" / "Período en curso: 2026-2". Contradicción reproducible entre pantallas del mismo rol sobre el mismo dato. | Capturas de /app/dashboard vs /app/decano/periodos vs /app/decano/mi-facultad/programas |
| F-DEC-02 | P2 | BACKEND (o FRONTEND, condición de vacío mal evaluada) | Seguimiento > Indicadores | El widget "Ausentismo por programa" muestra "No hay programas en esta facultad.", mientras la misma pantalla indica "Programas: 1" y la tabla de aforo de grupos lista "Ingenieria de Sistemas" en sus 7 filas. Verificado dos veces, sin ser un efecto de carga asíncrona. | Captura de pantalla /app/decano/seguimiento/indicadores |
| F-DEC-03 (mismo defecto que F-ADM-04, agravado) | P2 | FRONTEND (componente "vincular persona existente" reutilizado) | Mi Facultad > Coordinadores > Nombrar Coordinador Existente > Vincular | Mismo patrón sistémico: exige contraseña inicial para un docente que YA tiene cuenta (Jorge Iván Cardona Silva, jorge.cardona@uco.edu.co). Además, el mapeo de nombre aquí PIERDE datos por completo: "Jorge Iván Cardona Silva" → primerNombre="Jorge", primerApellido="Cardona", con segundoNombre y segundoApellido vacíos ("Iván" y "Silva" no quedan en ningún campo). No se completó el guardado. | Lectura de `value` de los inputs vía JS en el navegador |
| F-DEC-04 (menor) | P3 | FRONTEND | Panel Principal | El KPI "Coordinadores Asignados: 2" trae el subtítulo "Programas de facultad", que no corresponde al valor mostrado (2 son coordinadores, no programas; la facultad solo tiene 1 programa real). | Captura de pantalla /app/dashboard |

### Observación sin confirmar como defecto

- En Seguimiento > Consolidado, la suma de la columna "PÉRDIDA" por grupo (1+117+140+700=958) no coincide con el KPI "En pérdida: 675" de Indicadores. Podría deberse a deduplicación de estudiantes matriculados en varios grupos o a un criterio distinto entre ambas vistas; no se investigó la regla esperada por falta de contrato de referencia a la mano.

### Checks que PASARON sin hallazgos

Rol real confirmado; Mi Facultad (Programas con malla y plan, Áreas, Coordinadores con búsqueda en vivo y edición con prefill correcto sin pedir contraseña); Directorio (Docentes/Estudiantes/Asignaturas/Grupos correctamente acotados a la facultad, filtro de Programa funcional a diferencia del mismo filtro fallido para ADMINISTRADOR); Períodos Académicos; Seguimiento > Consolidado (lento ~5-7s, ya documentado, pero con datos correctos) y Cronograma (29 sesiones, aviso honesto de huso horario); Revisiones de Cancelación (832 registros, solo consulta, coherente con el Outbox visto en ADMINISTRADOR).

## Recorrido COORDINADOR (15/11/2026, navegador integrado, login manual del usuario)

Login manual como `uco-coordinador` (Andres Felipe Lopez Martinez, coordinador del programa Ingeniería de Sistemas). Rol real confirmado vía `GET /api/v1/auth/me` → `COORDINADOR`. Pantallas recorridas: Panel Principal, Programa (Grupos/Materias/Docentes/Períodos), Estudiantes del Programa, Asistencia (Consolidado/Solicitudes de sesión/Alertas), Revisiones de Cancelación, Planes de Estudio y Malla. No se completó ninguna escritura real: la prueba de "Nuevo grupo" quedó en 409 (rechazada por el propio backend) y "Nueva asignatura" se canceló tras validar.

### Hallazgos — COORDINADOR

| # | Severidad | Responsable probable | Pantalla/Acción | Resumen | Evidencia |
|---|---|---|---|---|---|
| F-COORD-01 | P1/P2 | FRONTEND o BACKEND (alcance de consulta distinto al de la pantalla de Estudiantes) | Panel Principal (dashboard) | El KPI "Estudiantes Registrados" muestra "0", pero la pantalla dedicada "Estudiantes del Programa" (mismo rol, mismo programa) muestra "5007 Estudiantes Registrados" / "Total en el Programa: 5007". Contradicción confirmada contra la pantalla real, no es un efecto de carga asíncrona. | Capturas /app/dashboard vs /app/coordinador/estudiantes |
| F-COORD-02 | P1/P2 | FRONTEND | Programa > Grupos > Nuevo grupo | El backend valida correctamente el cruce de horario de un docente (`POST /api/v1/grupos` → 409 `ERR_CRUCE_HORARIO_DOCENTE`, contrato limpio con correlationId), pero el frontend no muestra NINGÚN mensaje de error al usuario tras el rechazo: el formulario queda igual, sin toast ni indicación visible. El mismo silencio se repite con un código de grupo duplicado (409 genérico `code: "CONFLICT"`). El coordinador no tiene forma de saber por qué "Guardar" no funcionó. | `read_network_requests` con los dos 409; `read_console_messages` confirmando el error sin manejo en UI |
| F-COORD-03 | P2 | FRONTEND (sección sin implementar) | Asistencia del programa | La tarjeta "Alertas tempranas por estudiante" aparece vacía (solo el título, sin tabla ni mensaje de estado vacío) y no dispara ninguna petición HTTP relacionada, pese a que existen alertas reales en el sistema (outbox `ALERTA_PERDIDA_INASISTENCIA`, cientos de casos "en pérdida"). | `read_network_requests` con patrón "alerta" → 0 resultados |
| F-COORD-04 | P2 | FRONTEND | Revisiones de Cancelación (vista completa, "Solo pendientes" desmarcado) | En las 832 filas, la columna ESTADO dice "Pendiente de decisión del docente" mientras la columna ACCIÓN muestra el texto estático "Resuelta" (no es un botón). La pantalla se presenta como de decisión ("Decide si se cancela la matrícula...") pero no se encontró ningún control de decisión funcional en este entorno (ningún caso alcanzó la etapa "propuesta al coordinador"); la etiqueta "Resuelta" aplicada a filas no resueltas es, en sí misma, un defecto de presentación confirmado. | Captura de pantalla con ambas columnas visibles en la misma fila |
| F-COORD-05 (menor, mismo origen que F-ADM-02) | P3 | BACKEND (contrato de error inconsistente) | Programa > Grupos > Nuevo grupo (código duplicado) | El conflicto de código duplicado de grupo devuelve `code: "CONFLICT"` genérico en vez de un código específico como el de cruce de horario (`ERR_CRUCE_HORARIO_DOCENTE`); inconsistencia menor en la granularidad de los contratos de error. | Cuerpo de la respuesta 409 |

### Checks que PASARON sin hallazgos

Rol real confirmado; "Grupos en Curso" del dashboard SÍ reconoce el período vigente (contraste útil con F-DEC-01: no es un problema general de "período vigente", es específico del dashboard de decano); Programa > Grupos (horario semanal visible, validación nativa en submit vacío), Materias (nómina de grupo, catálogo curricular), Docentes (4 docentes, tipos de vinculación), Períodos; Estudiantes del Programa (5007, paginación, ficha con bitácora de asistencia y huso horario explícito "America/Bogota (UTC-05:00)", Corregir datos con prefill correcto sin pedir contraseña/correo); Asistencia > Consolidado (datos correctos, lento mismo patrón ya documentado) y Solicitudes de sesión (estado vacío correcto); Planes de Estudio y Malla (Nueva Asignatura con validaciones DEC-073 idénticas a ADMIN: créditos 1-10 y charset de nombre).

## Recorrido DOCENTE (15/11/2026, navegador integrado, login manual del usuario, a petición explícita del usuario fuera del orden original ADMIN→DECANO→COORDINADOR→ESTUDIANTE)

Login manual como `uco-docente` (Maria Elena Rostagno Valencia). Rol real confirmado vía `GET /api/v1/auth/me` → `DOCENTE`. Pantallas recorridas: Panel Principal, Mis Grupos (incluida la ficha de decisión por inasistencia), Control de Asistencia (toma de la Sesión #3, solo inspección, sin guardar), Reclamos (bandeja + detalle histórico), Mis Horarios, Mi Perfil. No se completó ninguna escritura real: la toma de asistencia está fuera de alcance de F2B y la ficha de "Nueva Sesión" se canceló tras validar.

### Hallazgos — DOCENTE

| # | Severidad | Responsable probable | Pantalla/Acción | Resumen | Evidencia |
|---|---|---|---|---|---|
| F-DOC-01 | **P1 — crítico** | FRONTEND | Mis Grupos > Grupo 001 > "Decisiones pendientes" > Decidir | El modal "Decisión de Inasistencia" (la pantalla que implementa literalmente "Decide si mantienes su matrícula o propones la cancelación al coordinador") no tiene NINGÚN control para elegir la acción. Confirmado por árbol de accesibilidad (`read_page`): el `form` contiene únicamente `label "Acción a tomar"` (sin radio/select/combobox asociado), `label "Motivo de la decisión"`, el textbox del motivo y los botones Cancelar/Continuar. "Continuar" queda deshabilitado permanentemente. Es un flujo normativo central completamente inoperable para todos los docentes en este entorno. | `read_page` (árbol de accesibilidad) + captura de pantalla del modal |
| F-DOC-02 | P2 | FRONTEND o BACKEND (criterio de "ejecutada" vs "programada" inconsistente) | Grupo 001 — tarjeta de sesión vs Control de Asistencia | La Sesión #3 (2026-12-09) se etiqueta "Programada" / "Próxima sesión" en la tarjeta del grupo, pero al abrir "Tomar asistencia" el sistema indica que la ventana YA está cerrada y que "el plazo para corregir esta toma terminó a las 10:30", con asistencia real ya registrada (6 Falta + 1 Excusa de 7) que es la base de la pérdida de Ana Sofia Gomez Rios. El resumen del grupo cuenta "3 programadas · 2 ejecutadas", dejando a la Sesión #3 fuera del conteo de ejecutadas pese a tener datos reales de ejecución. | Comparación de "Sesiones: 3 programadas · 2 ejecutadas" vs el estado real de la toma de la Sesión #3 |
| F-DOC-03 (sin acceso directo) | P2 | FRONTEND (navegación) | Panel Principal > "Revisiones pendientes de decisión" | El KPI del dashboard no ofrece ningún enlace directo a la pantalla real de decisión; fue necesario explorar "Mis Grupos", "Control de Asistencia" y el menú "Más acciones del grupo" → "Padrón y retiros" antes de localizarla. | Navegación registrada en esta sesión |
| F-DOC-04 (menor, sin confirmar) | P3 | A determinar | Mis Horarios | La suma manual de los bloques semanales mostrados (6.5 h) no coincide con el encabezado "8 Horas / Semana"; no se profundizó en la regla de cálculo esperada. | Captura de "Mi Horario Académico" |

### Checks que PASARON sin hallazgos

Rol real confirmado; Panel Principal (próxima clase calculada correctamente contra el horario real); Mis Grupos (4 grupos consistentes con los demás roles); Reclamos (bandeja con 9 casos históricos ya evaluados, detalle limpio sin datos sensibles); Mi Horario Académico (distribución semanal correcta salvo la nota P3); Mi Perfil; "+ Nueva Sesión" con validación correcta de hora fin anterior a inicio / cruce de medianoche, con zona horaria explícita "America/Bogota" (mejor que el aviso de incertidumbre visto en DECANO); "Tomar asistencia" con controles de guardado correctamente deshabilitados cuando la ventana está cerrada (sin riesgo de escritura accidental).

## Recorrido ESTUDIANTE (15/11/2026, navegador integrado, login manual del usuario)

Login manual como `uco-estudiante` (Carlos Andres Zapata Gomez). Rol real confirmado vía `GET /api/v1/auth/me` → `ESTUDIANTE`. Pantallas recorridas: Panel Principal, Mis Materias (Materias/Horarios/Oferta de grupos/Plan de estudios, detalle de sesiones y motivos de justificación), Mis Reclamos, Mis Alertas, Mi Perfil, y una prueba de protección de rutas. No se completó ninguna matrícula ni asistencia real (ambas fuera de alcance F2B); los intentos con PIN inválido/inexistente fueron rechazados por el propio backend sin mutar datos.

### Hallazgos — ESTUDIANTE

| # | Severidad | Responsable probable | Pantalla/Acción | Resumen | Evidencia |
|---|---|---|---|---|---|
| F-EST-01 | **P1/P2 — confirmado al 100 %, sin ambigüedad de timing** | FRONTEND | Panel Principal (dashboard) | "Materias Matriculadas: 0" y "No tienes materias inscritas para este periodo." pese a que `GET /api/v1/estudiante/materias` (la misma llamada que usa la pantalla "Mis Materias") responde correctamente con `{"exitoso":true,"datos":[{"nombreAsignatura":"Arquitectura de Software",...,"creditos":3}],"total":1}`. Se esperó explícitamente más de 6 segundos y se inspeccionó el cuerpo real de la respuesta antes de concluir: el backend entrega el dato, el componente del dashboard no lo procesa. La pantalla dedicada "Mis Materias" sí lo muestra correctamente (tras una demora de carga que ahí sí es de latencia, no de bug). | Cuerpo de la respuesta de `GET /api/v1/estudiante/materias` capturado en esta sesión |
| F-EST-02 | P2 (mismo patrón sistémico que F-ADM-02, F-COORD-02) | FRONTEND | Mis Materias > Matricularse con Código | Con un PIN de 6 dígitos válido en formato pero inexistente ("999999"), el backend responde `404 MQR_001` "El código de matrícula no es válido o ya no está disponible. Solicítale al docente uno nuevo." — contrato limpio y específico. El frontend no muestra ningún mensaje de error (confirmado por captura): el modal vuelve a su estado inicial en silencio. Cuarta confirmación del mismo patrón sistémico de "backend correcto, frontend silencioso" observado en ADMINISTRADOR, COORDINADOR y ahora ESTUDIANTE. | Captura de pantalla + cuerpo de la respuesta 404 |
| F-EST-03 (menor) | P3 | FRONTEND | Mis Materias > Matricularse con Código | Un PIN con letras ("ABCDE") es aceptado por el campo de texto (sin máscara numérica) y, al enviarlo, no se hace ninguna petición HTTP (bloqueo en JS) pero tampoco se muestra ningún mensaje de error. | Valor del campo confirmado por JS; ausencia de red de salida al enviar |

### Checks que PASARON sin hallazgos

Rol real confirmado; "Próxima Clase" calculada correctamente; Mis Materias (1 materia real con indicadores completos, 3 sesiones con historial detallado y 6 motivos de justificación), Oferta de grupos (distingue correctamente "Ya estás matriculado" de "usa el QR o PIN..."), Plan de estudios (marca "Cursando" correctamente), Mis Reclamos (6 históricos con soporte descargable y respuesta del docente), Mis Alertas (estado vacío explícito y correcto — contrasta positivamente con la sección sin implementar vista en COORDINADOR), Mi Perfil, y protección de rutas (acceso directo a una pantalla de ADMINISTRADOR redirige a `/app/acceso-denegado` con 403 y sesión persistente).

## Cierre consolidado de la certificación funcional (15/11/2026)

Cobertura: 5 roles recorridos con login manual real (ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE, ESTUDIANTE), 2 facultades de prueba creadas y 1 desactivada, 1 estudiante de prueba admitido, decenas de formularios probados con datos válidos e inválidos, y verificación cruzada contra Trazabilidad y Auditoría. No se ejecutaron suites automatizadas; todo fue interacción real en el navegador integrado.

### Hallazgos por severidad

**P0:** ninguno encontrado (no hubo exposición de datos, corrupción de información ni bloqueo de acceso generalizado).

**P1 — flujo principal roto:**
- **F-DOC-01**: el modal "Decisión de Inasistencia" del DOCENTE no tiene ningún control para elegir la acción a tomar; el flujo de "mantener matrícula o proponer cancelación" es inoperable para todos los docentes. **FRONTEND.**
- **F-ADM-01 / F-COORD-02**: el backend no impide que un mismo decano sea titular de dos facultades, y por otro lado sí valida correctamente cruces de horario de docentes en grupos — ambos casos comparten la causa raíz de que el **FRONTEND** no fuerza una elección real en combos críticos y no comunica los rechazos del backend.

**P1/P2 — patrón sistémico repetido en 4 pantallas distintas de 3 roles (ADMINISTRADOR, COORDINADOR, ESTUDIANTE):** el backend valida correctamente y devuelve contratos de error limpios (`ERR_PASSWORD_REQUERIDO`, `ERR_CRUCE_HORARIO_DOCENTE`, `CONFLICT`, `MQR_001`), pero el **FRONTEND** no muestra ningún mensaje al usuario en ninguno de los casos. Es la causa raíz más repetida de todo el recorrido.

**P1/P2 — KPIs de dashboard que contradicen la pantalla real correspondiente, en 3 de 4 roles con dashboard propio:**
- DECANO: "Sin período vigente" vs "Período en curso" en Programas/Períodos (F-DEC-01).
- DECANO: "Ausentismo por programa: no hay programas" vs "Programas: 1" (F-DEC-02).
- COORDINADOR: "Estudiantes Registrados: 0" vs "5007" en la pantalla real (F-COORD-01).
- ESTUDIANTE: "Materias Matriculadas: 0" vs 1 materia real confirmada por el cuerpo de la respuesta HTTP (F-EST-01).
- Nota: el dashboard de ADMINISTRADOR no mostró esta familia de defectos.

**P2 — secciones sin implementar o navegación sin salida directa:**
- COORDINADOR: "Alertas tempranas por estudiante" vacía sin ninguna petición HTTP asociada (F-COORD-03).
- COORDINADOR: columna ACCIÓN con la etiqueta estática "Resuelta" en filas cuyo ESTADO dice "Pendiente de decisión del docente" (F-COORD-04).
- DOCENTE: sin enlace directo desde el dashboard hasta la pantalla real de "Decisiones pendientes" (F-DOC-03).
- DOCENTE: una sesión con asistencia ya ejecutada se sigue mostrando como "Programada" (F-DOC-02).
- ADMINISTRADOR: vincular un docente/coordinador existente exige una contraseña nueva innecesaria y, en un caso, pierde el segundo nombre/apellido al mapear el nombre (F-ADM-04, F-COORD-03-decano, F-DEC-03).
- ADMINISTRADOR: filtro "Programa" en Docentes llama un endpoint de ámbito DECANO y queda vacío (F-ADM-03).

**P3 — menores:** inconsistencias de rotulado y mensajes de error faltantes en formularios puntuales (ver detalle en cada sección de rol); una nota sin confirmar sobre el cálculo de horas semanales del docente; una nota sin confirmar sobre la suma de "pérdida" en Consolidado vs el KPI de facultad.

### Responsabilidad probable

- **FRONTEND (Sergio)**: la inmensa mayoría de los hallazgos — el patrón sistémico de errores de backend no mostrados, los KPIs de dashboard desincronizados de sus propias pantallas, el modal de decisión de inasistencia sin control, el filtro de programas con endpoint equivocado, y los defectos de mapeo de nombre.
- **BACKEND**: ninguno identificado como causa raíz única y confirmada; en todos los casos probados el backend validó y respondió correctamente (incluso con mejor granularidad que el frontend llegó a mostrar). Única nota de higiene: la propiedad `security-baseline-pdp-admin.enabled` no se lee en ningún punto del código (posible deuda, no bloqueante).
- **DB / John**: sin hallazgos nuevos en este recorrido.
- **PROVEEDOR EXTERNO**: Catálogo de Parámetros y MessageUcoLab no disponibles en esta instalación (dependencia conocida, no se activó por instrucción explícita).

### Qué NO se certificó (NOT_RUN deliberado, no por imposibilidad técnica)

Cierre masivo de asistencia; alta/edición/eliminación del período académico activo; recarga de festivos; cambios estructurales de Seguridad UCO (roles/perfiles/administradores); envío real de correo de prueba (bloqueado por la política de la sesión del agente); matrícula real por PIN/QR; toma de asistencia real; resolución real de una revisión de cancelación o de un reclamo nuevo (no había casos disponibles en la etapa correcta); creación real de un grupo o asignatura nueva en producción.

### Recomendaciones concretas

1. Mostrar en el frontend el `message` (y cuando exista, `details[]`) de toda respuesta 4xx/409 del backend en formularios de creación — es la causa raíz más repetida de todo el recorrido.
2. Revisar y corregir los KPIs de los 4 dashboards (DECANO, COORDINADOR, ESTUDIANTE) para que consulten la misma fuente que sus pantallas de detalle.
3. Prioridad máxima: implementar el control de "Acción a tomar" en el modal de Decisión de Inasistencia del DOCENTE (F-DOC-01) — sin esto, ningún docente puede cerrar normativamente un caso de pérdida por inasistencia.
4. Definir y aplicar una regla de unicidad para "decano titular" (un decano, una facultad) o documentar explícitamente que la multiplicidad es válida.
5. Corregir el endpoint que alimenta el filtro "Programa" en Personas > Docentes (ADMINISTRADOR) para que no dependa de un endpoint de ámbito DECANO.
6. Implementar la sección "Alertas tempranas por estudiante" del COORDINADOR o retirarla mientras no exista.

### Cobertura que no pudo ejecutarse

Flujos cruzados que requieren un segundo rol simultáneo con fixtures sincronizados (p. ej. matricular un estudiante vía PIN y verificarlo en vivo desde el panel del coordinador) no se probaron de punta a punta porque habría requerido escrituras fuera de alcance de F2B. El rol DOCENTE se recorrió completo a solicitud explícita del usuario, fuera del orden original del plan.

## Evidencias remotas/manuales pendientes

- PR #154 (Fase 2A) se fusionó a `develop` del fork personal durante esta misma jornada (confirmado por `git fetch`: `develop` del fork avanzó por "Merge pull request #154 from alejovh12/fix/phase-2a-identity-conflict").
- El recorrido manual de los 5 roles quedó completo con login real (ver secciones "Recorrido <ROL>" y el cierre consolidado). Pendiente de owner: dictaminar los hallazgos P1/P2 listados y decidir si se amplía el alcance para implementarlos (fuera de DOCUMENTATION_ONLY).
- No se actualizó `MANUAL_VALIDATION_LEDGER.md` en esta pasada; queda pendiente para quien continúe esta fase.
- No se inventaron capturas; toda evidencia referenciada proviene de interacción real documentada en el momento (correlationId, cuerpos de respuesta HTTP, árbol de accesibilidad o captura de pantalla).
