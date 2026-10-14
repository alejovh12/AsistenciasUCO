# Auditoría de deuda técnica de seguridad — SEC-BASELINE-003

Inventario de auditoría, **no** un ledger paralelo: las entradas se proponen para el
[ledger único](../../baseline/TECHNICAL_DEBT.md) al integrar (próximo ID libre en `develop`: TD-059). Los IDs `SB3-*`
son locales a este work item. Se enlazan las TD existentes cuando la deuda ya estaba registrada.

**Origen:** (1) defecto propio de AsistenciasUCO · (2) defecto del proveedor Security Baseline · (3) contrato
institucional no acordado · (4) implementación frontend · (5) restricción exclusiva del laboratorio.

## Prioridad CRÍTICA

| ID | Deuda | Origen | Evidencia | Resolución esperada |
|---|---|---|---|---|
| SB3-01 | No existe fuente autoritativa ni aprovisionamiento/desaprovisionamiento automatizado de `idUsuario` en el IdP de Security Baseline; la asociación es manual | 3 | SEC-BASELINE-002 `IDENTITY_DECISION`; TD-013 | Contrato con Seguridad-UCO: fuente (SIS/SQL), alta/cambio/baja automatizados y auditados |
| SB3-02 | Sin issuer institucional definitivo: el PDP solo confía en su realm `security-baseline` (19090, HTTP, `keycloak.localhost`); el backend estable en el realm 8081 | 3 | `PEP-05`, `BE-04/05`, `E2E-07`, `ST-07` | Decisión de IdP único o federación por ambiente, con HTTPS ([MIGRATION_ADR](MIGRATION_ADR.md)) |

## Prioridad ALTA

| ID | Deuda | Origen | Evidencia | Resolución esperada |
|---|---|---|---|---|
| SB3-03 | **IDOR**: `GET /api/v1/grupos/{grupoId}/reportes/asistencia-excel` no valida titularidad; un DOCENTE ajeno descarga el reporte (nombres y asistencia) | 1 | `DL-03` (200, 4241 bytes); `GenerarReporteAsistenciaUseCaseImpl` sin usuario | Tarea separada FIX-002 (RED → GREEN; `InstitutionalScopePort` como en estudiantes/asistencias) |
| SB3-04 | PEP v0.1.6 responde `200` sin decisión cuando falta el Bearer (fail-open para clientes laxos) | 2 | `PEP-03` `KNOWN_UPSTREAM_FAIL`; mitigado: D-010/D-011, `E2E-11a` | Corrección upstream (401) |
| SB3-05 | Recursos PDP con plantillas `{grupoId}` → 500; solo rutas exactas (no escala: cada grupo/sesión/reclamo/soporte exige un recurso nuevo; el arnés los registra dinámicamente) | 2/3 | SEC-BASELINE-001; `role_matrix_e2e.py` (recursos dinámicos) | Soporte upstream de plantillas o modelo de recursos por capacidad + titularidad en la aplicación |
| SB3-06 | Revocación: cerrar las sesiones o deshabilitar al usuario no invalida el access token vigente (backend y PDP siguen permitiendo hasta `exp`, 300 s) | 1/3 | `REV-02`, `REV-04` `KNOWN_DESIGN_LIMIT`; `REV-03` (nuevo login rechazado) | Decidir TTL máximo, introspección/estado de usuario en el PDP o back-channel logout; documentar la ventana aceptada |
| SB3-07 | Credencial de aplicación del PEP estática en `lab.env`; sin secret manager ni rotación automatizada en ningún ambiente | 1/3 | `start-backend-pep-lab.ps1`; `SecurityBaselinePepProperties` (no la registra en logs) | Key Vault/secret manager + procedimiento de rotación con doble credencial |
| SB3-08 | Secretos literales en el release upstream (compose, realm importado, `application.properties` del PEP) | 2 | SEC-BASELINE-001 `RISKS` (rotados en el laboratorio) | Corrección upstream; rotación obligatoria en cualquier despliegue |
| SB3-09 | Autorización dinámica por recurso/propietario solo en AsistenciasUCO (`InstitutionalScopePort`); el PDP decide por ruta y no ve la query string (SSE `grupoId`) | 3 | `E2E-13a/13b`, `E2E-02*`, `SSE-*` | Mantener la titularidad en la aplicación como barrera obligatoria; acordar qué atributos ve el PDP |
| SB3-10 | ROPC (`grant_type=password`) habilitado en el cliente SPA `asistencias-uco-frontend` del realm 8081 (ya marcado `SECURITY DEBT` en `infra/keycloak`) y en clientes de laboratorio | 1/5 | realm-import y `bootstrap-keycloak.ps1`; `asistencias-lab-e2e` | Authorization Code + PKCE en el frontend; ROPC solo en laboratorio |
| SB3-11 | MFA administrativo no verificado (PDP/Keycloak de administración) | 2/3 | `NOT_RUN` | Política MFA institucional y prueba E2E |
| SB3-19 | Mapeo de grupos, facultades, programas y sedes inexistente en el PDP (no tiene modelo académico) | 3 | rol PDP por aplicación + rutas | Contrato: el PDP decide capacidades; el alcance académico queda en SQL/aplicación |
| SB3-20 | Gestión de permisos por rol manual (rol PDP por aplicación, asignaciones por usuario vía BFF) sin sincronización con los roles académicos | 2/3 | `MATRIZ_ROLES_E2E_LAB`, `DOCENTE_E2E_FIXPAG300_LAB` | Aprovisionamiento de roles/asignaciones desde la fuente institucional |
| SB3-21 | Frontend/BFF: el frontend usa el realm 8081; el BFF del PDP usa cookies `Secure` en `*.localhost`; la API excluye CSRF por ser Bearer-only | 4/3 | `SecurityConfig` (comentario CSRF), `CS-01/03` | Si se adopta BFF con cookie: reactivar CSRF y SameSite; PKCE en Angular |

## Prioridad MEDIA

| ID | Deuda | Origen | Evidencia | Resolución esperada |
|---|---|---|---|---|
| SB3-12 | Suscripciones SSE de clientes desconectados siguen contadas ~2 min (se detectan al fallar una escritura de heartbeat) | 1 | `E2E-13g` / observación del gauge | Detección de desconexión o timeout de emisión; relacionado con TD-003/TD-018 |
| SB3-13 | `ACCESS_DECISION_UNAVAILABLE` (503) solo en el overlay experimental, no en el contrato canónico | 1/3 | D-007; overlay `security-baseline-pep.overlay.yaml` | Aprobación contractual antes de merge |
| SB3-14 | `POST /api/v1/realtime/emit` fuera del OpenAPI; datos inválidos → 500 por `ResponseStatusException` | 1 | FIX-001 `PLAN` (fuera de alcance) | Retirar o contratar; mapear a 400 |
| SB3-15 | `environment` del PEP no ligado a la aplicación (`prod` permitido igual que `local`) | 2 | `PEP-14` | Corrección upstream |
| SB3-16 | El scope upstream `security-baseline-claims` añade `security-baseline-bff` al `aud` de tokens de usuario de la API | 2 | `role-matrix-provisioning.json`, `e2e.jsonl.txt` | Audiencias mínimas por cliente o token exchange |
| SB3-17 | Imágenes del proveedor en un namespace personal de Docker Hub sin firma; `mssql/server:2022-latest` (tag mutable); Keycloak lab 26.7.2 vs institucional 26.7.1 | 2/5 | `docker inspect` (digests en [AS_IS](AS_IS.md)) | Registro institucional, firma/SBOM, tags inmutables por digest |
| SB3-18 | Observabilidad: el backend no propaga `X-Decision-Id` en denegaciones del PEP ni correlaciona logs del PEP/PDP en Loki | 1/2 | filas `E2E-03`, `E2E-13c` sin decisión | Registrar el decision ID de denegaciones y enviar logs del PEP/PDP al stack de observabilidad |
| SB3-22 | La copia de DB no es 100 % sintética (6 semillas con dominio institucional, 1 `uco.net.co`) | 1/5 | [DB_ISOLATION](DB_ISOLATION.md#datos-sensibles) | Fixture reproducible 100 % sintético para entornos compartidos/CI |
| SB3-23 | Vistas concedidas al rol runtime exponen columnas de contraseña (`uv_usuario.password`, `uv_administrador.passwordUsuario`) | 3 (owner DB) | columnas de vistas (lectura) | Contrato DB: vistas sin hash o con permiso de columna |
| SB3-24 | Muchas operaciones declaran `FEATURE_UNAVAILABLE` (501): listado/creación/borrado de reclamos del estudiante, listado de reclamos del docente, perfil de usuario, docentes del coordinador, sedes, **todos** los DELETE | 1/4 | `RC-03`, `DEL-*`, `RB-*` (501) | Backlog funcional; el frontend no puede listar reclamos |
| SB3-25 | Persistencia/DR del laboratorio y del proveedor: volúmenes Keycloak/SurrealDB/Redis sin backup; backups SQL solo dentro del volumen del contenedor | 2/5 | inventario Docker | Procedimiento de backup/restore y RPO/RTO por componente antes de producción |
| SB3-26 | Identidad institucional E2E del ADMINISTRADOR (`E2E_ADMIN_ID_USUARIO`, 8081) no existe en la DB | 1 | `role-identity-verification.json` | Alinear el seed E2E institucional con la DB |

## Prioridad BAJA

| ID | Deuda | Origen | Evidencia | Resolución esperada |
|---|---|---|---|---|
| SB3-27 | Política documentada `/api/v1/decano/**` = DECANO o ADMINISTRADOR, pero `ConsultarCoordinadoresUseCaseImpl` exige la facultad del decano autenticado (ADMINISTRADOR → 403, fail-closed) | 1 | `RB-admin GET /api/v1/decano/coordinadores` `KNOWN_DEFECT` | Decidir la política y alinear documentación o caso de uso |
| SB3-28 | Respuestas 401/403/503: el 403 del PEP y el de Spring/Application comparten `code=FORBIDDEN`; solo `X-Decision-Id` distingue la capa | 1 | atribución de capas en [SECURITY_E2E_MATRIX](SECURITY_E2E_MATRIX.md) | Aceptable; documentar para soporte |
| SB3-29 | Cobertura E2E ≈37 de ~110 operaciones | 1 | [TEST_PLAN](TEST_PLAN.md#not_run) | Ampliar por contrato |
| SB3-30 | `grant_type=password` en el arnés de laboratorio (`asistencias-lab-e2e`, institucional E2E) | 5 | `lab_matrix.py`, `role_matrix_e2e.py` | Prohibido fuera del laboratorio (ya documentado) |

## Resolubles por AsistenciasUCO sin depender de Seguridad-UCO

SB3-03 (FIX-002), SB3-06 (decisión de TTL/estrategia y parte de implementación), SB3-07 (secret manager), SB3-10
(PKCE con el frontend), SB3-12, SB3-13 (decisión contractual propia), SB3-14, SB3-18 (lado backend), SB3-22,
SB3-24, SB3-26, SB3-27, SB3-28, SB3-29.

## Dependen de Seguridad-UCO (proveedor o contrato institucional)

SB3-01, SB3-02, SB3-04, SB3-05, SB3-08, SB3-09 (atributos del PDP), SB3-11, SB3-15, SB3-16, SB3-17 (imágenes), SB3-19,
SB3-20, SB3-21 (parte BFF), SB3-23 (owner DB), SB3-25 (componentes del proveedor).
