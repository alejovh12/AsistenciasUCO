# Servicio de Notificaciones UCO 0.1.1

Componente **independiente** que envía los correos institucionales: recibe avisos por HTTP de las aplicaciones cliente
(hoy, el backend de Asistencias desde su outbox), mantiene las **plantillas versionadas**, las renderiza, las entrega por SMTP con
cola, reintentos y tope de envíos, y expone la administración (versionar, activar, vista previa, cola, reintento, correo de
prueba y estado del canal).

> Decisión del responsable (2026-11-22): vive en `Complementos/Notificaciones/` junto a Seguridad, CatalogoMensajes y
> CatalogoParametros, **sin repositorio git propio** (es una carpeta del proyecto, pensada para el futuro monorepo). Plan completo:
> `docs/PLAN_COMPONENTE_NOTIFICACIONES.md`.

## Contenido

| Carpeta / archivo | Qué es |
|---|---|
| `Guia-integracion-Notificaciones.md` | Contrato de uso para cualquier aplicación cliente (autenticación, idempotencia, errores, plantillas, límites). |
| `contrato/openapi-notificaciones.yaml` + `.sha256` | Contrato OpenAPI 3.0 congelado (WORM). Una prueba del servicio verifica que las rutas coincidan y que el hash corresponda. |
| `notificaciones-uco/` | Código del servicio: Spring Boot 4.0.8 / Java 25, hexagonal, JPA + Flyway sobre SQL Server, SMTP con Jakarta Mail, Maven Wrapper, `Dockerfile` y pruebas. |
| `notificaciones-local/` | Kit de laboratorio: `compose.yaml`, `.env.example`, `scripts/crear-bd.ps1` y su README. |

## Arquitectura (resumen)

```
POST /api/v1/notificaciones ─► GestionNotificaciones ─► NotificacionRepositoryPort ─► JPA (tabla Notificacion, idempotente)
                                      │ despierta
                                      ▼
                         DespachadorScheduler (hilo) ─► Despachador: reserva con lease, entrega, backoff, depuración
                                                              │
                                    EnviadorCorreo ◄──────────┘   tipo entregable, destinatarios, modo de pruebas, BCC, TopeEnvios
                                      │            └─► RenderizadorPlantillas (versión activa → versión 1 de fábrica → texto guardado) + layout
                                      ▼
                              CorreoTransportPort ─► JakartaMailCorreoTransport (SMTP)  |  LogCorreoTransport (sin SMTP)
```

- **Dominio y aplicación** (`co.edu.uco.notificaciones.domain`, `.application`): sin Spring ni JPA; casos de uso detrás de puertos
  (`NotificacionesUseCase`, `PlantillasUseCase`; salidas `NotificacionRepositoryPort`, `PlantillaRepositoryPort`,
  `CorreoTransportPort`, `MetricasPort`, `DespertadorPort`). El Composition Root es `infrastructure.config.AplicacionConfig`.
- **Infraestructura**: controladores REST, adaptadores JPA (sin SQL inline salvo el bloqueo `UPDLOCK/READPAST` que genera Hibernate),
  transporte SMTP, filtros `X-Correlation-Id` y `X-Api-Key` (comparación en tiempo constante), Actuator + Prometheus, OTLP listo.
- **BD propia** `notificacionesdb` (Flyway: `V1` esquema, `V2` siembra de los 22 tipos con sus marcadores y la versión 1 activa de
  cada plantilla, tomadas de los recursos del backend y del catálogo de la BD de Asistencias; `V3` corrige la ortografía de las
  descripciones de los tipos).
- **Lease**: el despachador corta el ciclo si al lote reservado le queda menos margen del necesario (mayor entre 5 s acotado a
  lease/4 y el doble del envío más lento) y libera las reservas no procesadas sin contar el intento (corrección del outbox de
  noviembre: una reserva vencida no se reenvía por otra instancia mientras la primera sigue enviando).

## Avisos agrupados y saludo genérico (por diseño)

Un aviso con varios destinatarios (p. ej. `SESION_CANCELADA` a todo el grupo) se renderiza **una sola vez** y se entrega en un
único mensaje con los destinatarios en BCC (bloques de `SMTP_NOTIFICATION_MAX_BCC`, sujetos al tope de envíos). Por eso los datos
son comunes a todos y el consumidor debe enviar un valor genérico (p. ej. `estudiante: "estudiantes del grupo"`). No hay
renderizado por destinatario: para saludar por nombre, el consumidor debe emitir un aviso por persona (dentro del tope).

## Cómo construirlo y probarlo

Requisitos: JDK 25 y Docker (las pruebas de integración levantan un SQL Server real con Testcontainers; sin Docker se omiten, y
entonces el gate de JaCoCo no se cumple).

```powershell
cd Complementos\Notificaciones\notificaciones-uco
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\jtmp'     # fix de loopback de Java en Windows
.\mvnw.cmd verify                                                  # pruebas + JaCoCo (LINE >= 80 %, BRANCH >= 70 %)
docker build -t notificaciones-uco:0.1.1 .                         # imagen (no ejecuta las pruebas de integración)
```

## Cómo levantarlo en el laboratorio

Ver `notificaciones-local/README.md`: crear la BD (`scripts/crear-bd.ps1`), copiar `.env.example` a `.env`, `docker compose up -d` y
probar `GET http://localhost:18090/actuator/health`. La sección `ext-notificaciones` del stack de Infra (fase F3 del plan) hará esto
mismo desde `infra.ps1 stack up`.

## Cómo reemplazarlo

Otro equipo puede entregar su componente de notificaciones: mientras cumpla `contrato/openapi-notificaciones.yaml`, solo cambian el
adaptador del backend (URL y clave) y la sección del stack. Si el contrato cambia, se publica un addendum con su hash nuevo.

## Pendientes conocidos

- Autenticación entre servicios: hoy `X-Api-Key` (deuda técnica nueva: OAuth2 client-credentials cuando Seguridad lo ofrezca).
- Un único idioma sembrado (`es`); la API ya acepta `idioma`.
- El código no está respaldado en git: respaldo `.zip` fechado al cerrar cada fase (riesgo del plan, sección 6).
