---
status: active
type: runbook
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Conexion SQL Server local

## Arquitectura de ejecucion

- Backend: Spring Boot 4.0.6, ejecutar desde la raíz de este repositorio con Java 25.
- Servidor principal: Spring MVC sobre servlet.
- Concurrencia: virtual threads habilitados.
- Persistencia: Spring JDBC contra SQL Server.
- Commands: Stored Procedures.
- Queries: Views.
- Realtime: Reactor Core está activo para SSE sobre Spring MVC; no hay starter WebFlux. El core sigue imperativo/JDBC.
- Base esperada: `gestionasistenciadb`.
- URL JDBC esperada: `jdbc:sqlserver://localhost:1433;databaseName=gestionasistenciadb;encrypt=true;trustServerCertificate=true`.

## Secretos

Las credenciales se cargan desde `.env`, ignorado por Git. No se debe versionar ni imprimir su contenido.

Formato esperado:

```properties
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=gestionasistenciadb;encrypt=true;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=sa
SPRING_DATASOURCE_PASSWORD=<valor local>
APP_DATABASE_EXPECTED_NAME=gestionasistenciadb
```

## Scripts locales

- `scripts/load-env.ps1`: carga variables al proceso sin imprimir valores.
- `scripts/run-local.ps1`: valida variables obligatorias y ejecuta `.\mvnw.cmd spring-boot:run`.
- `scripts/test-local.ps1`: valida variables obligatorias y ejecuta `.\mvnw.cmd verify -Pintegration`.

## Comandos

```powershell
.\mvnw.cmd test
.\scripts\test-local.ps1
.\scripts\run-local.ps1
```

## Comprobaciones de integracion

`SqlServerConnectionIT` verifica conexion, `SELECT 1`, `DB_NAME()`, usuario SQL, version de SQL Server, existencia de vistas requeridas y que los puertos productivos principales no usen mocks.

`GrupoRepositorySqlServerIT` valida el SP real de registro de estudiante y comprueba rollback real consultando la base despues de un fallo.

`EstudianteRepositorySqlServerIT` valida consultas reales de solo lectura para listado paginado y detalle de estudiantes.

## Contratos SQL consumidos por el backend (confirmar versión desplegada)

- `dbo.usp_registrar_estudiante_en_grupo_usuario_no_existente`
- `dbo.uv_grupo`
- `dbo.uv_tipo_identificacion`
- `dbo.uv_usuario`
- `dbo.uv_docente_identidad`
- `dbo.uv_docente`

## Contrato de errores SQL

El ejecutor Java espera que los SPs retornen `estadoResultado`, `mensajeUsuarioResultado` y `mensajeTecnicoResultado`. No existe `codigoResultado`.

El backend clasifica los fallos funcionales exclusivamente en infraestructura, usando operacion + mensajes SQL, y propaga codigos internos `ERR_*`. `mensajeTecnicoResultado` solo se usa para diagnostico/log y no sale por REST.

## Deuda de seguridad de password

El hallazgo histórico y la evidencia posterior de `PasswordEncoderPort`/`UsuarioPasswordHashSqlServerIT` se consolidan en [TD-020](../baseline/TECHNICAL_DEBT.md#td-020). No afirmar credenciales planas actuales ni validación runtime sin evidencia. Nunca imprimir valores para comprobarlo.
