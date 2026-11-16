---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-16
---

# VAL-oleada7-backend - PLAN

Fecha: 2026-11-15. Rama `fix/VAL-oleada7-backend` desde `repo-antiguo/develop` (aa553578); el 2026-11-16 pasa a
`fix/VAL-oleada7-cache-pep` sobre develop 8530bd54. El ajuste de la ruta literal del inventario del PDP se retiró: lo cubre
el PR #153 (el extractor ya resuelve constantes). Plan completo de la oleada en
Infra: `docs/work-items/VAL-oleada7-pendientes-transversales/PLAN.md` (pasos C y D). Sin cambios de contratos, de BD ni
de frontend.

| Paso | Hallazgo | Cambio | Archivos |
| :--- | :--- | :--- | :--- |
| C | H4 (VAL-oleada3-carga): con el PEP deteniéndose, una petición en vuelo esperó más de 30 s pese a `request-timeout: 5s` | Prueba en rojo con socket crudo; la consulta al PEP pasa a `sendAsync(...).get(request-timeout)` y al vencer cancela el intercambio (UNAVAILABLE, fail-closed) | `SecurityBaselinePepClient`, `SecurityBaselinePepClientTest` |
| D | F12-06 / REA-03: cachés de parámetros y mensajes sin vencimiento; con dos réplicas la edición solo llega a una | `CacheConVencimiento` (reloj inyectable, TTL `app.adapters.sql-catalog-cache.ttl`, 60 s, máximo 24 h) en los proveedores `sqlserver` | `CacheConVencimiento`, `ParameterCatalogJpaRepository`, `MessageCatalogJpaRepository`, `application.yml`, `CatalogoSqlCacheVencimientoTest` |

Fuera de alcance: H1 (caché de decisiones del PDP, Security Baseline), LB-005 (SSE y límite de subidas por instancia),
TD-093 (unicidad de personas y roles), docente.
