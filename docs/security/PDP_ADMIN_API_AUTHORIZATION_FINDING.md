---
status: active
type: security-finding
scope: backend
owner: backend-team
last-reviewed: 2026-10-26
---

# Hallazgo: la API administrativa del PDP 0.1.7 autentica pero no autoriza

**Clasificación:** `SECURITY_FINDING` en el componente externo Security Baseline (Seguridad UCO). **Estado:**
`BLOCKED_BY_EXTERNAL_COMPONENT`. **Consecuencia en este repositorio:** `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false`.

## Evidencia reproducible (2026-10-26, laboratorio local PDP 0.1.7, solo lectura)

Sonda `GET` contra el PDP (`127.0.0.1:18080`) con el Bearer de cada cuenta de laboratorio. No se imprimen tokens ni cuerpos.

| Identidad | `GET /api/v1/applications` | `GET /api/v1/users` |
|---|---|---|
| ADMINISTRADOR | 200 | 200 |
| DOCENTE | 200 | 200 |
| ESTUDIANTE | 200 | 200 |
| Sin Bearer | 401 | — |

Cualquier token válido del realm lee el directorio administrativo del PDP: la API exige *autenticación* pero no
*autorización administrativa*. No se probaron escrituras (acción privilegiada sobre un recurso compartido no autorizada).

## Por qué no se habilita la sincronización

El backend necesita escribir asignaciones de perfil (`POST /api/v1/profiles/{id}/assignments`). Con esta API, una
credencial de servicio con privilegio mínimo no se puede distinguir de la de un docente; habilitarla exigiría
conceder o asumir más privilegio del aprobado y reutilizar secretos que no corresponden. Se mantiene:

- `SECURITY_BASELINE_PDP_PROFILE_SYNC_ENABLED=false` (valor por defecto en `application.yml`).
- Secreto propio `SECURITY_BASELINE_PDP_ADMIN_CLIENT_SECRET`, nunca el de administración del proveedor de identidad.
- Las cuentas creadas quedan `PENDIENTE_SINCRONIZACION`; la SPA lo muestra y nunca declara `ACTIVA` sin perfil confirmado.

## Decisiones que debe tomar Seguridad UCO

1. Que la API administrativa del PDP aplique autorización efectiva (rol/scope/audiencia administrativos) y responda 403
   a tokens de docente y estudiante.
2. Definir la identidad de servicio del backend (client credentials), su audiencia y el scope mínimo
   (`profiles:assign`/`profiles:revoke` sobre la aplicación `asistencias-uco`, sin lectura global de usuarios).
3. Decidir el mecanismo oficial de aprovisionamiento durante el desarrollo (por ejemplo `provision_asistencias.py users`
   ejecutado por el responsable) mientras 1 y 2 no estén aprobados.

## Condiciones para declarar `FULL_PDP_SYNC_READY`

Sonda con 403 para DOCENTE/ESTUDIANTE; cuenta de servicio con scope mínimo comprobado; reconciliación real de alta,
cambio y revocación de rol; sin acceso administrativo indebido. Hasta entonces el desarrollo ordinario continúa con las
cuentas de laboratorio ya migradas a perfiles y el alta de roles por el procedimiento del responsable.
