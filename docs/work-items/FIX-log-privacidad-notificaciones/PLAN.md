---
status: active
type: plan
scope: backend
owner: backend-team
last-reviewed: 2026-10-31
---

# FIX — privacidad de logs de notificaciones

Base: develop `acc1f2c9db24a339adabf6b112f3a634c67b44fb`; remoto personal en el mismo SHA. El usuario autoriza expresamente este fix y exige omitir campos desconocidos y datos personales de los logs. Clase: BEHAVIOR_CHANGE; variable principal: contenido observable del log.

AS-IS: los dos adaptadores log serializan valores arbitrarios de `datos`, que IDENT-001 usa para el nombre. TARGET: solo metadatos técnicos de tipo, identificador y destinatarios contractualmente admitidos; ningún valor o clave de `datos`, asunto ni cuerpo. El mapa funcional continúa intacto para outbox, renderer y SMTP.

Rutas autorizadas: este work item, `docs/baseline/LINEA_BASE.md`, los dos adaptadores log, sus tests y las enmiendas de privacidad del contrato y test plan NOTIF-001. Prohibidas: `.codex/`, FIX-003, FIX-002, SQL, Frontend, configuración runtime, puertos y providers SMTP. Consumidores: operadores de logs, outbox, IDENT-001 y NOTIF-001/002A; no cambia contrato HTTP ni persistencia. Rollback: revertir el diff de este fix. Riesgos: pérdida deliberada de información arbitraria del mapa en logs; preservar tipo, identificadores y logging técnico. Detener ante otro conflicto contractual o RED que no falle por privacidad.

## Dictamen de contrato y DoR

`TEST_CONTRACT_CONFLICT`: el test histórico exige `grupoId=123` en el log; NOTIF-001 describe claves de datos y prohíbe nombres/correos. La nueva decisión expresa del usuario omite campos desconocidos y valores del mapa. Auditoría: la aserción histórica contradice la política de privacidad autorizada; el tester la reemplaza **antes del RED** por la ausencia del valor, reforzando el requisito, y conserva los demás asserts. Esta decisión acota y sustituye la expectativa histórica de registrar datos para este fix; no cambia el puerto ni el payload funcional. No modificar los tests tras congelar el RED.

DoR: READY para este alcance: contrato y criterios dados por el usuario, test plan adjunto, CI base `Backend Quality Gate` y ArchUnit exitosos en el SHA base; prueba unitaria base local PASS con Java 25. Integraciones reales no necesarias para esta modificación aislada y no autorizadas. Cierre depende de RED, GREEN y gates registrados en VALIDATION.
