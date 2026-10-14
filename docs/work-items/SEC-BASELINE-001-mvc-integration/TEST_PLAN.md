# Plan de pruebas

## RED

- Cliente PEP: serializa exactamente el request raw, usa el endpoint y headers contractuales, no reenvía `Cookie`, y clasifica `2xx`, `401`, `403`, `5xx`, timeout y errores de transporte.
- Filtro MVC: no llama al PEP con flag apagado/ruta no protegida/sin autenticación; permite continuar solo con respuesta PEP permitida; mapea indisponibilidad a `503`; conserva `X-Decision-Id` cuando el PEP lo entrega.
- Wiring real (`SecurityBaselinePepWiringTest`, PEP simulado a nivel HTTP): una sola instancia en `FilterChainProxy`, justo después de `BearerTokenAuthenticationFilter` y antes de `AuthorizationFilter`; registro servlet deshabilitado (`ServletContextInitializerBeans`); una evaluación por petición; 403/401/503 sin ejecutar el handler; 403 local con PEP allow; rutas públicas y preflight sin PEP; ruta fuera de prefijos exige decisión; dispatch ASYNC sin reevaluación; cookie no reenviada; correlación saneada; sin `Upgrade: h2c`.
- Propiedades (`SecurityBaselinePepPropertiesTest`): HTTP solo loopback, origen fijo, timeouts acotados, `toString` sin credencial, flag OFF sin beans, URL insegura falla el arranque.
- Mutaciones verificadas: sin `FilterRegistrationBean` deshabilitado, aceptar cualquier 2xx y quitar HTTP/1.1 rompen los tests.
- Higiene: no se escriben bearer tokens, cookies, credenciales ni cuerpos sensibles en logs.

## GREEN

Implementar lo mínimo en Infrastructure y Composition Root hasta que las pruebas focales anteriores pasen. No cambiar tests RED para acomodar la implementación.

## VALIDATE

- `.\mvnw.cmd -B -ntp -Dtest=... test` para el foco.
- `.\mvnw.cmd -B -ntp verify` para la rama, registrando cualquier interrupción o skip.
- `git diff --check`, búsqueda de secretos y revisión de que no aparezca WebFlux nuevo.
- Docker solo con configuración no destructiva; sin `down -v`.
- Matriz real con `infra/security-baseline-lab/scripts/lab_matrix.py` (`pep`, `backend`, `backend-pep-down`).
