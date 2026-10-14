# Riesgos y controles

| Riesgo | Control | Estado |
|---|---|---|
| Bypass del PEP por doble registro del filtro (`a74b288`) | `FilterRegistrationBean` deshabilitado + test de wiring (`FilterChainProxy`, `ServletContextInitializerBeans`) + mutación verificada | **Corregido** en `16621c7` |
| `SECURITY_FINDING` upstream: PEP v0.1.6 responde `200` sin decisión cuando no hay Bearer (fail-open para clientes que aceptan cualquier 2xx) | ALLOW solo con `204` + `X-Decision-Id`; sin Bearer no se consulta. Reportar al equipo de Security Baseline | Mitigado localmente; abierto upstream |
| PEP responde `501` a `Upgrade: h2c` del cliente JDK | HTTP/1.1 explícito + aserción de regresión | Corregido |
| Bypass si PEP no responde | Fail-closed `503`; verificado con el contenedor detenido | Controlado |
| Cambio accidental de JWT/roles | Filtro adicional después de Bearer; decoder/converter/reglas sin cambios; 403 local con PEP allow (SIM) | Controlado |
| Confundir `sub` con actor institucional | No mapear sin contrato | Abierto — `BLOCKED_BY_IDENTITY_CONTRACT` |
| CSRF por introducir cookies | No se reenvía Cookie; no se activa BFF/cookie | Controlado |
| Exposición de secretos | Secretos aleatorios en `%USERPROFILE%\.asistencias-security-lab\lab.env` (ACL usuario); `toString` enmascarado; escaneo 0 coincidencias | Controlado |
| `SECURITY_FINDING` upstream: secretos literales en `distribution/compose.yaml`, en el realm importado (clientes `security-baseline-bff`, `security-pep-internal`, usuario `security-demo`) y en `pep/src/main/resources/application.properties` (`pep.pdp.service-identity.client-secret`) | No reutilizados: rotados en el Keycloak del laboratorio, demo deshabilitado, secreto del PEP inyectado por variable. Valores no reproducidos | Controlado en el laboratorio; abierto upstream |
| `environment` no vinculado a la aplicación en el PDP (PEP-14: `prod` permitido igual que `local`) | Documentado; el backend fija el valor por configuración | Abierto upstream (informativo) |
| Recursos con plantilla `{x}` → `500` en PDP v0.1.6 | Rutas exactas en el laboratorio; impide modelar `/grupos/{id}/...` | Abierto upstream |
| Correlación del PEP sin sanear | Solo el valor de `CorrelationIdContext` + validación de formato | Controlado |
| Incompatibilidad WebFlux/MVC | Cliente servlet JDK; `pom.xml` sin cambios | Controlado |
| HTTP hacia un PEP compartido | HTTP solo a loopback; fuera de loopback HTTPS obligatorio al arrancar | Controlado |
| Imagen Docker no oficial | Procedencia correlacionada con el workflow oficial; digests registrados | Controlado (sin firma criptográfica upstream) |
| Laboratorio comparte estado con estable | Proyecto/red/volúmenes/loopback separados; servicios ajenos sin reinicios | Controlado |
| Contrato documental incompleto | Manuales PDF ausentes en el release | Abierto |
| Código `ACCESS_DECISION_UNAVAILABLE` fuera del contrato publicado | Solo con flag ON; requiere decisión contractual antes de merge (D-007) | Abierto |
