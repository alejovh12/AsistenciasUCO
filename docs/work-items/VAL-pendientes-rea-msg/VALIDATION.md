---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-13
---

# VAL-pendientes-rea-msg (Backend) - VALIDATION

Fecha: 2026-11-13. Rama local `fix/VAL-pendientes-rea-msg` sobre `origin/develop` 29d66aaf. Windows, Java 25, `mvnw.cmd`. La
compuerta de BD se corrió después, nunca en paralelo con Maven.

## Compuerta del backend

| Comando | Resultado |
| :--- | :--- |
| `./mvnw.cmd -B verify` | `BUILD SUCCESS`, rc 0, 25:38 min |
| Pruebas (Surefire) | 4502 ejecutadas, 0 fallos, 0 errores, 0 omitidas |
| ArchUnit (`architecture/*`) | incluidas en las 4502, todas en verde |
| JaCoCo | `All coverage checks have been met` (agregado del reporte: líneas 94,98 %, ramas 86,39 %) |

## RED → GREEN

| Prueba | RED (antes del cambio) | GREEN |
| :--- | :--- | :--- |
| `ManagementPortActuatorSecurityTest` sin `ManagementEndpointsSecurityConfiguration` | 2 fallos: `prometheus...: expected <200> but was <401>` (reproduce H2) y `otras_rutas...: expected <403> but was <401>` | 5 de 5 |
| `SubirArchivoAntivirusNoDisponibleTest` con el `InternalApplicationException` anterior en `SubirArchivoUseCaseImpl` | 5 de 5 `Status expected:<503> but was:<500>` (reproduce H3) | 5 de 5 |
| `ManagementPortRequestMatcherTest`, `ServiceUnavailableExceptionTest` | — (nuevas) | 4 de 4, 3 de 3 |
| `MensajeUsuarioErrorCodeCatalogoTest` (deuda vacía, fixture con 492 códigos) | — | 4 de 4 |
| `MessageUcoLabCatalogoTest` (492 códigos, 120 alias) | — | 5 de 5 |
| `SubirArchivoUseCaseImplTest`, `CircuitBreakerMalwareScanAdapterTest`, `ManagementPortConfigTest` (existentes) | — | 17, 3 y 3 en verde, sin cambios |

Los dos RED se obtuvieron revirtiendo temporalmente solo la línea correspondiente (import de la configuración en la prueba, o el
`throw` del caso de uso) y restaurando el archivo después.

## Catálogo

| Verificación | Resultado |
| :--- | :--- |
| `build-message-catalog.py --seed <BD>/schema/seed --previo <fixture> --psv` | `mensajes=492 alias=120`; los 26 alias anteriores sin cambio |
| `build-message-catalog.py --seed ... --previo <fixture> --check <fixture>` | `OK ... coincide con los seeds (492 mensajes)` |
| Textos nuevos de más de 100 caracteres | ninguno (los tres que se pasaban se acortaron) |

## Limitaciones

- La prueba de H2 levanta Tomcat real con la cadena real de la API, pero con un `JwtDecoder` simulado y sin el PEP de Security
  Baseline; no es una corrida contra Prometheus de Infra. Falta el smoke en el lab: `curl http://127.0.0.1:8090/actuator/prometheus`
  (200) y el target `asistencias-backend` en `UP` en Prometheus.
- La prueba de H3 usa el adaptador ClamAV real contra sockets locales (puerto cerrado y servidor que no responde) y el breaker
  real; no se apagó un ClamAV real. Re-medir en el lab con el contenedor de ClamAV detenido (esperado: 503
  `ERR_ANTIVIRUS_NO_DISPONIBLE`).
- El fixture se valida contra los seeds de la rama de BD; hasta que se una la migración 42, el catálogo de la BD de desarrollo
  no tiene esos textos y el backend responde con el texto de respaldo del enum (mismo significado).
- Sin push ni CI de GitHub.
