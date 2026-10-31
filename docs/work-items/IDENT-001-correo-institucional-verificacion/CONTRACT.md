---
status: frozen-approved
type: contract
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-31
---

# CONTRACT — IDENT-001: Correo institucional y verificación del correo

Fase 02. **Estado: `DRAFT` (sin `.sha256`).** Pasa a `CONTRACT_FROZEN` cuando el owner lo apruebe. Deriva del [PLAN](PLAN.md) (D1–D10 aprobadas). Los puntos marcados **[CONFIRMAR F03]** se verifican contra el código al escribir las pruebas RED y no cambian el comportamiento descrito.

---

## 1. Matriz AS-IS / TARGET

| Elemento | AS-IS | TARGET | Compatibilidad |
|---|---|---|---|
| Parámetros `REGISTRO/DOMINIO_CORREO_ESTUDIANTE`, `VERIFICACION_CORREO/*` | — | Nuevos (§2.1) | Aditivo |
| `ufn_correo_cumple_dominio_institucional` | — | Nueva (§2.2) | Aditivo |
| `dbo.TokenVerificacionCorreo` | — | Nueva (§2.3) | Aditivo |
| `usp_registrar_estudiante_en_grupo` | Sin validación de dominio | Valida dominio en altas nuevas (§2.4) | **Cambia comportamiento** (firma igual) |
| `usp_emitir_token_verificacion_correo`, `usp_validar_token_verificacion_correo`, `usp_confirmar_verificacion_correo`, `usp_preparar_reenvio_verificacion_correo` | — | Nuevos (§2.5–2.8) | Aditivo |
| Mensajes `VCE_001`–`VCE_006` | — | Nuevos (§2.9) | Aditivo |
| Plantilla `VERIFICACION_CORREO` (+ marcadores) | — | Nueva, semilla v1 (§3.4) | Aditivo |
| `GET /api/v1/configuracion/registro-estudiante` | — | Nuevo (§4.1) | Aditivo |
| `POST /api/v1/grupos/{grupoId}/estudiantes` | Acepta cualquier correo | 400 si el correo no es institucional (§4.2) | **Cambia comportamiento** |
| `POST /api/v1/public/verificar-correo` | — | Nuevo, público (§4.3) | Aditivo |
| `POST /api/v1/grupos/{grupoId}/estudiantes/{estudianteId}/reenviar-verificacion` | — | Nuevo (§4.4) | Aditivo |
| `CrearCuentaIdentidadDTO` | 7 campos | + `requiereVerificacionCorreo` (constructor de 7 campos se conserva) (§3.2) | Compatible |
| `IdentityProviderPort` | — | + `confirmarCorreoCuenta` (opcional, `default` no soportado) (§3.2) | Compatible |
| `Notificacion` | 4 tipos | + `TIPO_VERIFICACION_CORREO` (§3.3) | Aditivo |
| Keycloak (estudiante registrado por el docente) | `enabled=true`, `emailVerified=true` | `enabled=false`, `emailVerified=false` hasta verificar | **Cambia comportamiento** (solo ese flujo) |

---

## 2. Base de datos

### 2.1 Reglas comunes y parámetros
- Reglas del contrato de NOTIF: `SET XACT_ABORT ON`, transacción explícita con savepoint cuando aplique, `IS NULL`, mensajes por código y tupla canónica `idResultado, exitoso, mensajeUsuario, mensajeTecnico` en los comandos. Sin literales de estado ni GUIDs en T-SQL.
- Semilla idempotente `MERGE` en `dbo.CatalogoParametro` (`grupo`, `clave`, `valor`, `tipoDato`, `valorDefecto`):

| grupo | clave | valor | tipoDato | Uso |
|---|---|---|---|---|
| `REGISTRO` | `DOMINIO_CORREO_ESTUDIANTE` | `@uco.edu.co` | STRING | Dominio permitido (incluye `@`) |
| `VERIFICACION_CORREO` | `TTL_HORAS` | `24` | INT | Vigencia del enlace |
| `VERIFICACION_CORREO` | `REENVIO_ESPERA_MINUTOS` | `5` | INT | Espera mínima entre reenvíos |

### 2.2 `dbo.ufn_correo_cumple_dominio_institucional(@correo NVARCHAR(255), @dominio NVARCHAR(255)) RETURNS BIT`
- Normaliza ambos con `LOWER(LTRIM(RTRIM()))`.
- Devuelve `1` solo si: el correo contiene **exactamente un** `@`; la parte local es no vacía; el sufijo del correo desde el `@` es **igual** al dominio (que debe empezar con `@` y tener al menos un carácter después). Cualquier otro caso (incluido `NULL` o dominio vacío) devuelve `0`.
- Ejemplos: `a@uco.edu.co` → 1; `A@UCO.EDU.CO ` → 1; `a@a.uco.edu.co`, `a@uco.edu.co.evil.com`, `@uco.edu.co`, `a@@uco.edu.co`, `a@uco.edu.co@x.com` → 0.

### 2.3 `dbo.TokenVerificacionCorreo` (migración `10_ident001_*` + `schema/tables/`)
```sql
id                 UNIQUEIDENTIFIER PK DEFAULT NEWID(),
usuario            UNIQUEIDENTIFIER NOT NULL -> FK Usuario(id),
tokenHash          VARBINARY(32)    NOT NULL,   -- SHA-256; el token en claro nunca se guarda
fechaEmisionUtc    DATETIME2(0)     NOT NULL,
fechaExpiracionUtc DATETIME2(0)     NOT NULL,
fechaUsoUtc        DATETIME2(0)     NULL,
fechaRevocacionUtc DATETIME2(0)     NULL,
CONSTRAINT UQ_TokenVerificacionCorreo_tokenHash UNIQUE (tokenHash)
-- índice único filtrado: (usuario) WHERE fechaUsoUtc IS NULL AND fechaRevocacionUtc IS NULL  → un token activo por usuario
```
Sin columna de estado ni literales: «activo» = no usado, no revocado y `SYSUTCDATETIME() < fechaExpiracionUtc`. Migración `down` incluida. Permisos de ejecución en `rol_asistencias_runtime_ident001.sql` (sin DML directo sobre la tabla).

### 2.4 `usp_registrar_estudiante_en_grupo` (firma sin cambios)
Se agrega, **después** de la validación de RBAC y de existencia de grupo/cupo y **antes** de sincronizar el usuario:
- Si **no existe** un usuario con ese correo y ese documento (alta nueva): resolver `REGISTRO/DOMINIO_CORREO_ESTUDIANTE` y llamar a `ufn_correo_cumple_dominio_institucional`. Si devuelve 0 → error `VCE_001` con el dominio como parámetro del mensaje; `exitoso = 0`; sin escrituras.
- Si ya existe (mismo correo y documento): no se revalida (D5).
- Sin otros cambios; `usp_sincronizar_usuario_interno` ya inserta `correoConfirmado = 0`.

### 2.5 `usp_emitir_token_verificacion_correo` (técnico, sin ejecutor)
- Parámetros: `@idUsuario`, `@tokenHash VARBINARY(32)`, `@idCorrelacion`.
- Lo invoca el canal de correo al entregar. Valida: usuario existente y `estado` activo; `correoConfirmado = 0` (si ya está verificado → `VCE_005`).
- Efectos (una transacción): revoca (`fechaRevocacionUtc`) el token activo previo; inserta el nuevo con `fechaExpiracionUtc = DATEADD(HOUR, <TTL_HORAS>, SYSUTCDATETIME())`.
- Resultset: tupla canónica. No devuelve el hash ni el token.

### 2.6 `usp_validar_token_verificacion_correo` (consulta pública, sin ejecutor)
- Parámetros: `@tokenHash`, `@idCorrelacion`. Solo lee; no consume.
- Éxito: tupla canónica + `idUsuario`. Falla con `VCE_002` (**mismo mensaje y mismo resultado** para inexistente, vencido, usado, revocado o usuario inactivo).

### 2.7 `usp_confirmar_verificacion_correo` (comando público, sin ejecutor)
- Parámetros: `@tokenHash`, `@idCorrelacion`.
- Una transacción con `UPDLOCK, HOLDLOCK`: revalida el token (igual que §2.6), marca `fechaUsoUtc`, pone `Usuario.correoConfirmado = 1`. Si el token ya no es válido → `VCE_002`. Éxito → mensaje `VCE_006`.

### 2.8 `usp_preparar_reenvio_verificacion_correo`
- Parámetros: `@idGrupo`, `@idEstudiante`, `@idUsuarioEjecutor`, `@idCorrelacion`.
- RBAC `DOCENTE,COORDINADOR` y ámbito: docente **titular** del grupo, o coordinador del programa del grupo (aislamiento jerárquico dentro del SP). Fuera de ámbito → `VCE_004`.
- Reglas: el estudiante pertenece al grupo; `correoConfirmado = 0` (si no → `VCE_005`); si el último token se emitió hace menos de `REENVIO_ESPERA_MINUTOS` → `VCE_003`.
- Resultset: tupla canónica + `idUsuario` (el backend registra la notificación). No emite el token (se emite al entregar).

### 2.9 Mensajes (`CatalogoMensajeUsuario` y `CatalogoMensajeTecnico`, semilla `27_mensajes_ident001.sql`; marcador `{}` según la convención vigente)
| Código | Tipo | Mensaje al usuario |
|---|---|---|
| `VCE_001` | BUSINESS_ERROR | El correo del estudiante debe ser institucional y terminar en {}. |
| `VCE_002` | BUSINESS_ERROR | El enlace de verificación no es válido o ya venció. |
| `VCE_003` | BUSINESS_ERROR | Debe esperar unos minutos antes de volver a enviar el enlace de verificación. |
| `VCE_004` | BUSINESS_ERROR | El estudiante no pertenece a un grupo a su cargo. |
| `VCE_005` | BUSINESS_ERROR | El correo de este estudiante ya fue verificado. |
| `VCE_006` | SUCCESS | Su correo fue verificado. Ya puede iniciar sesión. |

---

## 3. Backend

### 3.1 Dominio institucional
- `DominioCorreoInstitucionalPolicy` (Application, puro): normaliza y aplica la **misma regla** que §2.2. Lee el dominio con `ParameterCatalogPort.getRequiredParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE")`.
- `RegistrarEstudianteUseCaseImpl`: invoca la política **solo en altas nuevas** (cuando `validarConflictoIdentidadUsuario` devuelve `false`), antes del SP. Falla con una excepción de validación que mapea a HTTP 400 con el mensaje `VCE_001` desde `MessageCatalogPort`.
- El SP sigue siendo la autoridad; la política evita el viaje a la BD.

### 3.2 Identidad (Keycloak)
- `CrearCuentaIdentidadDTO` gana el campo `boolean requiereVerificacionCorreo`; el constructor de 7 argumentos se conserva y equivale a `false` (los demás roles no cambian).
- Con el flag en `true`, `KeycloakIdentityProviderAdapter` crea el usuario con `enabled=false` y `emailVerified=false`. La contraseña se establece como hoy.
- `IdentityProviderPort.confirmarCorreoCuenta(String idExterno)` (método `default` que lanza «no soportado»). Keycloak: `PUT /users/{id}` con `{"enabled":true,"emailVerified":true}`; idempotente.
- Una cuenta que **ya existía** en Keycloak (`newlyCreated=false`) no se modifica.

### 3.3 Notificación
- `Notificacion.TIPO_VERIFICACION_CORREO = "VERIFICACION_CORREO"`. Solo `idUsuario` del estudiante (rol `ESTUDIANTE`) y `datos` con claves legibles `nombre` y `grupo`. **Nunca** token ni correo.
- Se emite al registrar un alta nueva (después de `provisionarIdentidad`, mejor esfuerzo, no revierte el registro) y al reenviar.
- Exactamente **un** destinatario por notificación de este tipo.

### 3.4 Canal de correo (cambio aditivo en `notification`)
- SPI interno `MarcadorDinamicoProvider` (`notification/template`): `Map<String,String> resolver(String tipo, UUID idUsuario)`; el canal lo invoca al entregar y mezcla el resultado en `datos` antes de renderizar. Para tipos sin proveedor devuelve vacío.
- `VerificacionCorreoMarcadorProvider`: genera 32 bytes con `SecureRandom` (base64url), calcula el SHA-256, llama a `usp_emitir_token_verificacion_correo` y devuelve `{ "enlace": "<platform-url>/verificar-correo#token=<token>" }`. El token no se registra en logs ni métricas.
- Si la emisión falla por `VCE_005` (ya verificado) → `NotificacionNoEntregableException` (no se reintenta). Cualquier otro fallo de BD → transitorio (el despachador reintenta y emite un token nuevo, que revoca el anterior).
- `SmtpNotificacionChannelSender`: para `VERIFICACION_CORREO` **no** aplica el filtro `require-confirmed-email` (el destinatario aún no está confirmado por definición).
- Plantilla v1 sembrada (`26_plantillas_notificacion.sql` se extiende o se agrega `28_plantilla_verificacion_correo.sql`): asunto «Verifique su correo institucional»; cuerpo con `{nombre}` y `{enlace}`; marcadores obligatorios `nombre`, `enlace`; recurso por defecto `notification/templates/VERIFICACION_CORREO.es.properties`. Se escapa todo valor sustituido (regla D6 de NOTIF-002A).

### 3.5 Verificación (aplicación)
Flujo `VerificarCorreoUseCase` (puertos primarios/secundarios nuevos, sin SQL ni `EntityManager` en `application`):
1. `usp_validar_token_verificacion_correo(hash)` → `idUsuario`.
2. `IdentityProviderPort.buscarCuentaPorIdUsuario(idUsuario)` → `confirmarCorreoCuenta(idExterno)`. Si Keycloak falla → error controlado; el token **sigue vigente**.
3. `usp_confirmar_verificacion_correo(hash)` consume el token y marca `correoConfirmado = 1`.
El hash SHA-256 lo calcula el adaptador de persistencia; Application maneja el token como un valor opaco.

### 3.6 Límite de intentos y seguridad web
- Filtro en memoria sobre `POST /api/v1/public/verificar-correo`: por IP (`remoteAddr`; `X-Forwarded-For` se ignora), 20 solicitudes por minuto (propiedad `app.verificacion-correo.limite-por-minuto`), memoria acotada; exceso → 429.
- `SecurityConfig`: `permitAll` **solo** para `POST /api/v1/public/verificar-correo` (sin comodín `/public/**`). `GET /api/v1/configuracion/registro-estudiante` y el reenvío requieren `DOCENTE` o `COORDINADOR`. Se actualiza `access-catalog.json` y `HTTP_AS_IS_MATRIX`.
- Observabilidad: contadores `identity.email_verification.issued|verified|rejected` (etiquetas de baja cardinalidad); logs con `correlationId` y sin token ni correo.

---

## 4. HTTP

### 4.1 `GET /api/v1/configuracion/registro-estudiante`
`200` → `ApiDataResponse<{ "dominioCorreo": "@uco.edu.co" }>`. Roles: `DOCENTE`, `COORDINADOR`. Errores: 401, 403.

### 4.2 `POST /api/v1/grupos/{grupoId}/estudiantes` (existente)
Sin cambios en el cuerpo ni en la respuesta de éxito. Nuevo: `400` con `mensajeUsuario` = `VCE_001` cuando el correo de un alta nueva no es institucional.

### 4.3 `POST /api/v1/public/verificar-correo`
- Cuerpo: `{ "token": "<base64url, 43 caracteres>" }`. Validación de formato en el validator del controlador (longitud y alfabeto); no se invoca el caso de uso si es inválido.
- `200` → `ApiDataResponse<Void>` con `mensajeUsuario` = `VCE_006`.
- `400` → mensaje `VCE_002` (**idéntico** para token inexistente, vencido, usado, revocado o malformado).
- `429` → exceso de intentos. Sin autenticación; sin datos personales en ninguna respuesta.

### 4.4 `POST /api/v1/grupos/{grupoId}/estudiantes/{estudianteId}/reenviar-verificacion`
- Roles: `DOCENTE`, `COORDINADOR`. Cuerpo vacío.
- `200` → `ApiDataResponse<Void>` (el aviso queda en la cola). Errores: `403/404` fuera de ámbito (`VCE_004`), `409` ya verificado (`VCE_005`), `429` espera mínima (`VCE_003`).

---

## 5. Frontend (**BLOQUEADO** hasta tener el repositorio; contrato de comportamiento)
- Formulario de enrolamiento manual: consulta §4.1 una vez, valida el correo contra el dominio recibido antes de enviar y muestra el `mensajeUsuario` del backend ante `400`. Sin dominio escrito en el código. Componentes `OnPush`, `input()`/`output()`, sin `setInterval`.
- Página pública `verificar-correo`: lee `#token=` del fragmento, lo envía por §4.3 y muestra el `mensajeUsuario` devuelto. Borra el fragmento de la URL tras leerlo (`history.replaceState`). No guarda el token.
- Acción «Reenviar enlace» para el docente en la lista de estudiantes (§4.4).

---

## 6. Pruebas existentes que cambian (anticipo)
Solo por dato de prueba (correo no institucional), sin tocar aserciones: ver PLAN §4. Cualquier otra contradicción → `TEST_CONTRACT_CONFLICT`.

## 7. Rollback
Migración `10_ident001_*_down.sql` (elimina tabla, función, SPs y parámetros); revertir el SP de registro; el flag `requiereVerificacionCorreo` en `false` restablece la conducta anterior de Keycloak.

## 8. Puntos a confirmar en Fase 03 (no cambian el comportamiento)
- Convención exacta del marcador de parámetros en `usp_obtener_mensaje_catalogo` (hoy `{}`).
- Qué adaptador de `ParameterCatalogPort` (BD o Azure) está activo en cada perfil; el valor sembrado en BD es el SSOT para el SP.
- Nombre de las propiedades de configuración y su ubicación en `application.yml` / `.env.template`.
