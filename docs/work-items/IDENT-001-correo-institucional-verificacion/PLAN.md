---
status: frozen-approved
type: plan
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-31
---

# PLAN — IDENT-001: Correo institucional obligatorio y verificación del correo en el registro manual del estudiante

Fase 01 (Planificación y Definition of Ready). **Estado: `FROZEN`: el owner aprobó D1–D10 el 2026-10-31 («Procede con el plan»).** Siguiente: [CONTRACT](CONTRACT.md) y [TEST_PLAN](TEST_PLAN.md); no se escribe código ni pruebas hasta que el owner apruebe el CONTRACT.

- **Historia de usuario afectada:** HU050 (matrícula/registro manual del estudiante por el docente, hoy `[CERTIFICADA]`). Modifica un comportamiento certificado: requiere contrato formal.
- **Base Git:** rama `feature/NOTIF-002A-plantillas-correo` (backend `8162b44`, BD `bf6e3f1`). Sin merge con `develop`. Ramas de trabajo: `feature/IDENT-001-correo-institucional-verificacion` en backend y BD.
- **Programas relacionados:** REG-000 (DA-5, contraseña asignada en la creación; **se conserva**), TD-014 (`emailVerified=true` supuesto; **este work item la resuelve para el estudiante registrado por el docente**), NOTIF-001/002A (outbox, plantillas y SMTP; **se reutilizan**).

---

## 1. Objetivo y alcance

### Dentro
1. **Correo institucional obligatorio** al registrar manualmente a un estudiante (HU050). El dominio es una variable (parámetro), hoy `@uco.edu.co`. La validación existe en **tres capas**: procedimiento almacenado, backend y frontend.
2. **Verificación del correo** por enlace: el estudiante recibe un correo con un enlace de un solo uso; hasta abrirlo no puede iniciar sesión. **La contraseña se asigna como hoy** (la envía el docente en el registro); este work item no la cambia.
3. Reenvío del enlace por el docente (necesario para no dejar cuentas atrapadas por un enlace vencido).

### Fuera (seguimientos)
- Registro por QR de matrícula (HU176B): reutilizará este mecanismo, pero se planifica aparte.
- Asignación de la contraseña por el estudiante en el enlace (queda para una fase posterior).
- Purga automática de cuentas que nunca verifican (seguimiento TD).
- Docentes, coordinadores, decanos y administradores (sus altas no cambian).
- Recuperación de contraseña y cambio de correo.

### Decisiones del owner ya tomadas (2026-10-31)
| Tema | Decisión |
|---|---|
| Variable del dominio | Parámetro en BD (`dbo.CatalogoParametro`), leído por el SP y por el backend; el frontend lo obtiene por un endpoint de configuración nuevo |
| Formato | Un solo sufijo/dominio (`@uco.edu.co`), comparación exacta e insensible a mayúsculas |
| Verificación | **Token propio**, enviado con el outbox de NOTIF (no el `VERIFY_EMAIL` de Keycloak) |
| Rama base | `feature/NOTIF-002A-plantillas-correo` |
| Alcance de la regla | Solo el registro manual por el docente; la contraseña no cambia |

---

## 2. AS-IS (evidencia)

| Elemento | AS-IS |
|---|---|
| Endpoint | `POST /api/v1/grupos/{grupoId}/estudiantes` (`GrupoController`); el cuerpo incluye `password` |
| Caso de uso | `RegistrarEstudianteUseCaseImpl`: DB primero (`GrupoRepositoryPort.registrarEstudianteEnGrupo`), luego `IdentityProviderPort.crearCuenta`; el hash va a la DB y la clave en claro a Keycloak |
| SP | `usp_registrar_estudiante_en_grupo` (RBAC `DOCENTE,COORDINADOR`; valida grupo y cupo) → `usp_sincronizar_usuario_interno` + `usp_registrar_estudiante_en_grupo_interno`. **No valida el dominio del correo** |
| Keycloak | `KeycloakIdentityProviderAdapter.crearCuenta` crea el usuario con `enabled=true` y **`emailVerified=true`** (TD-014); la clave se fija con `reset-password` |
| BD | `Usuario.correoConfirmado` existe; `usp_sincronizar_usuario_interno` lo inserta (línea 172) sin verificación real |
| Dominio | No hay validación de dominio institucional en el SP, el backend ni el frontend (búsqueda de `uco.edu.co` en `src/main`: sin resultados) |
| Correo | Existe el outbox, las plantillas editables y el canal SMTP (NOTIF-001/002A), con modo de pruebas |
| Parámetros | `dbo.CatalogoParametro` (`grupo`, `clave`, `valor`, `tipoDato`) y `ParameterCatalogPort` en `application/secondaryports/catalog` |
| Frontend | **No está clonado** en este workspace (`GestioAsistenciaFrontend`). `BLOCKED_BY_MISSING_EVIDENCE` para la fase de frontend |

---

## 3. TARGET

### 3.1 Dominio institucional (3 capas)
- **BD:** parámetro `('REGISTRO', 'DOMINIO_CORREO_ESTUDIANTE', '@uco.edu.co', 'STRING')` sembrado de forma idempotente. Función `ufn_correo_cumple_dominio_institucional(@correo, @dominio)`: normaliza con `LOWER(TRIM())`, exige exactamente un `@`, parte local no vacía y dominio **igual** al configurado (así `x@uco.edu.co.evil.com` y `x@a.uco.edu.co` se rechazan). La invoca `usp_registrar_estudiante_en_grupo` **antes de crear nada**, con mensaje de catálogo y el dominio como parámetro del mensaje. Sin dominio quemado en T-SQL.
- **Backend:** `ParameterCatalogPort` entrega el dominio; un validador de aplicación rechaza antes de llamar al SP (HTTP 400, mensaje desde `MessageCatalogPort`). Misma regla de normalización que el SP; el SP sigue siendo la autoridad.
- **Frontend:** el formulario de enrolamiento manual consulta el dominio a un endpoint de configuración y valida antes de enviar. Los mensajes que se muestran son los que devuelve el backend; el dominio no se escribe en el código del cliente.
- **Endpoint nuevo (lectura):** `GET /api/v1/configuracion/registro-estudiante` → `{ dominioCorreo }`, accesible para `DOCENTE` y `COORDINADOR`. Cambia la superficie HTTP: se documenta en `HTTP_AS_IS_MATRIX` y requiere CONTRACT.
- **Aplicación de la regla:** solo en **altas nuevas**. Un estudiante que ya existe (mismo correo y documento) se matricula sin revalidar, para no romper cuentas previas con otro dominio (D5).

### 3.2 Verificación del correo
```
Docente registra al estudiante ─► SP (valida dominio, crea usuario con correoConfirmado=0, matricula)
        │
        ├─► Keycloak: crearCuenta(enabled=false, emailVerified=false)   ← no puede iniciar sesión
        └─► NotificacionPort(tipo=VERIFICACION_CORREO, destinatario=idUsuario)  ← outbox
                                         │
Despachador ─► (al entregar) emite token de un solo uso ─► plantilla ─► SMTP ─► correo con enlace
                                         │
Estudiante abre  {platform-url}/verificar-correo#token=…  ─► frontend lee el fragmento
        └─► POST /api/v1/public/verificar-correo {token}
              1. SP valida el token (hash, vigencia, un solo uso), sin consumirlo
              2. Keycloak: habilitar cuenta + emailVerified=true  (idempotente)
              3. SP confirma: consume el token y pone Usuario.correoConfirmado=1
```
- **Token:** 32 bytes de `SecureRandom`, en base64url. En la BD solo se guarda su **SHA-256** (`dbo.TokenVerificacionCorreo`: `id`, `usuario`, `tokenHash`, `estado`, `fechaEmisionUtc`, `fechaExpiracionUtc`, `fechaUsoUtc`); índice único por hash; un token activo por usuario (emitir otro revoca el anterior). Vigencia por parámetro (propuesta: 24 h).
- **Enlace con fragmento (`#token=`):** el token no viaja en la petición HTTP de la página ni queda en logs del servidor ni en `Referer`; el frontend lo lee y lo envía en el cuerpo de un `POST`.
- **El token no se guarda en el outbox (D7):** el outbox registra solo `tipo` + `idUsuario` (sin token). El token se emite **al momento de entregar** y se inserta en el marcador `{enlace}` de la plantilla. Así ninguna fila del outbox (retención de 30 días) contiene un token utilizable.
- **Endpoint público** `POST /api/v1/public/verificar-correo`: ruta permitida sin sesión **solo para esa ruta** (no se abre un comodín `/public/**`), con límite de intentos por IP, respuesta indistinguible entre token inexistente, vencido y usado, y sin PII en la respuesta.
- **Reenvío:** `POST /api/v1/grupos/{grupoId}/estudiantes/{estudianteId}/reenviar-verificacion` (docente titular o coordinador del programa), con espera mínima entre reenvíos (parámetro) y sin revelar si el correo ya estaba verificado a terceros.
- **Orden y fallos:** DB primero, Keycloak después (patrón vigente). Si Keycloak falla al verificar, el token **no se consume** y el enlace se puede reintentar (el paso de Keycloak es idempotente). El fallo del correo no revierte el registro (mejor esfuerzo, ya garantizado por NOTIF).
- **Contraseña:** sin cambios. Se envía como hoy; la cuenta queda deshabilitada hasta verificar el correo.

---

## 4. Impacto en pruebas existentes (se anticipa para no tratarlo como sorpresa)
La nueva regla es un cambio formal de contrato. Estas pruebas registran estudiantes con correos que no son institucionales y habrá que actualizarlas **solo donde su dato de prueba deje de ser válido**, con la justificación en el PR (sección «Tests existentes modificados»):
- **BD:** `test_usp_registrar_estudiante_en_grupo_usuario_no_existente.sql`, `test_transaction_ownership.sql`, `verify_runtime_role_login.ps1`.
- **Backend:** `RegistrarEstudianteUseCaseImplTest`, `RegistrarEstudianteInteractorTest`, `GrupoControllerWriteEndpointsTest`, `GrupoRepositorySqlServerIT`, `SesionGrupoCommandsSpParityIT` (GRP_04 usa `@example.test`) y las demás de `RegistrarEstudiante*`/`GrupoJpa*` que arman un correo de prueba.
- **Regla:** no se relajan aserciones ni se eliminan casos. Si una prueba contradice el contrato nuevo → `TEST_CONTRACT_CONFLICT`: se detiene, se documenta y se consulta antes de tocarla.

---

## 5. Decisiones propuestas (D1–D10)

| ID | Decisión | Propuesta |
|---|---|---|
| D1 | Dominio como parámetro `REGISTRO/DOMINIO_CORREO_ESTUDIANTE`, valor `@uco.edu.co`, comparación exacta e insensible a mayúsculas | Sí |
| D2 | La validación se aplica en SP, backend y frontend, con la misma normalización; el SP es la autoridad | Sí |
| D3 | El frontend obtiene el dominio de `GET /api/v1/configuracion/registro-estudiante` (sin dominio en el código del cliente) | Sí |
| D4 | Mensajes de rechazo desde el catálogo (`Mensaje`), con el dominio como parámetro | Sí |
| D5 | La regla aplica a **altas nuevas**; un estudiante existente (mismo correo y documento) no se revalida | Sí |
| D6 | Verificación con token propio de 32 bytes, solo su SHA-256 en BD, un solo uso, un activo por usuario, vigencia 24 h por parámetro | Sí |
| D7 | El token se emite **al entregar** el correo y no se persiste en el outbox | Sí |
| D8 | Cuenta Keycloak del estudiante registrado manualmente creada `enabled=false` y `emailVerified=false`; se habilita al verificar. Los demás roles no cambian | Sí |
| D9 | Enlace con fragmento `#token=`; verificación por `POST` público con límite de intentos y respuesta uniforme | Sí |
| D10 | Incluir el reenvío del enlace con espera mínima por parámetro | Sí |

---

## 6. Riesgos
| Riesgo | Mitigación |
|---|---|
| Cambia una HU certificada (HU050) | CONTRACT formal; pruebas existentes actualizadas solo por dato de prueba, con justificación |
| Tocar el módulo de NOTIF (marcador dinámico `{enlace}` resuelto al entregar) aún sin publicar | Cambio aditivo y aislado en `notification`; reglas ArchUnit AR-07..AR-10 siguen vigentes |
| Cuenta atrapada si el correo no llega o el enlace vence | Reenvío (D10); seguimiento de purga |
| Enumeración o abuso del endpoint público | Respuesta uniforme, límite por IP, ruta pública acotada, token de alta entropía |
| Fallo de Keycloak tras confirmar | El token no se consume hasta que Keycloak responde; paso idempotente |
| Enlace al entorno equivocado | `SMTP_NOTIFICATION_PLATFORM_URL` obligatorio y validado (ya existe) |
| Correos reales en pruebas | Se usa el modo de pruebas con redirección de NOTIF-002A |
| Frontend sin repositorio local | Fase 3 bloqueada hasta tener la URL del repo; las fases de BD y backend avanzan |

---

## 7. Fases (TDD estricto, parada al terminar cada una)
| Fase | Contenido | Verificación |
|---|---|---|
| 02 CONTRACT | Contrato de BD (parámetro, función, tabla, SPs), HTTP (2 endpoints nuevos + reenvío) y mensajes; congelar con `.sha256` | Aprobación del owner |
| 03 TEST RED | Pruebas T-SQL, unitarias backend y ArchUnit, derivadas del contrato | RED real antes de GREEN |
| 04 DB | Parámetro, función, tabla, SPs, permisos de runtime, registro en la suite | Gate de BD en verde (hoy 376/375/0/1) |
| 05 Backend | Validador de dominio, emisión de token, marcador `{enlace}`, endpoints, ajuste de Keycloak, wiring | `mvnw test` e integración vs. línea base (2864 unitarias; 258 IT) |
| 06 Frontend | Validación con el dominio del endpoint y página `verificar-correo` | Bloqueada hasta tener el repo |
| 07 E2E y cierre | Registro manual con GreenMail, verificación, reenvío y vencimiento; `VALIDATION.md` y deuda técnica | Evidencia con cifras reales |

---

## 8. Criterios de aceptación
| ID | Criterio |
|---|---|
| CA-01 | Registrar manualmente un estudiante con correo que no termina en el dominio configurado devuelve 400 con mensaje del catálogo; no se crea usuario, estudiante, matrícula ni cuenta |
| CA-02 | El dominio se lee del parámetro; cambiarlo en BD cambia la validación en SP, backend y frontend sin recompilar |
| CA-03 | `x@uco.edu.co.evil.com`, `x@a.uco.edu.co`, dos `@` o parte local vacía se rechazan; mayúsculas y espacios alrededor se normalizan |
| CA-04 | Un registro válido deja al estudiante matriculado, `correoConfirmado=0`, la cuenta deshabilitada en Keycloak y un aviso `VERIFICACION_CORREO` en el outbox sin token |
| CA-05 | El correo entregado contiene un enlace con fragmento `#token=`; en la BD solo existe el SHA-256 del token |
| CA-06 | Abrir el enlace habilita la cuenta, marca `emailVerified=true` y `correoConfirmado=1`, y consume el token; reabrirlo falla con el mismo mensaje que un token inexistente |
| CA-07 | Un token vencido, usado, revocado o inexistente es indistinguible para quien llama |
| CA-08 | El estudiante no puede iniciar sesión antes de verificar y sí después, con la contraseña definida por el docente |
| CA-09 | El reenvío revoca el token anterior, respeta la espera mínima y solo lo pueden usar el docente titular o el coordinador del programa |
| CA-10 | Ningún log, métrica, evento ni fila del outbox contiene el token ni el correo en claro |
| CA-11 | Si Keycloak falla al verificar, el token sigue siendo válido y el reintento funciona |

---

## 9. Definition of Ready
| Punto | Estado |
|---|---|
| 1. Objetivo y alcance | ✅ §1 |
| 2. AS-IS con evidencia | ✅ §2 (revisado el 2026-10-31) |
| 3. Decisiones del owner | ✅ D1–D10 aprobadas (2026-10-31) |
| 4. Contratos de BD y HTTP | ⏳ [CONTRACT](CONTRACT.md) en borrador, pendiente de aprobación |
| 5. Pruebas existentes afectadas identificadas | ✅ §4 |
| 6. Frontend | ❌ `BLOCKED_BY_MISSING_EVIDENCE`: repo no clonado |
| 7. Rollback | Aditivo salvo `Keycloak enabled/emailVerified` del estudiante nuevo y el SP de registro; revertible por migración inversa |

**Veredicto: `NOT_READY` hasta que el owner apruebe el CONTRACT** (y, para la fase de frontend, se entregue el repositorio). No se implementa nada antes.
