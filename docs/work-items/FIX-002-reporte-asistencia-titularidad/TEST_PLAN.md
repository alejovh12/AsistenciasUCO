---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-30
---

# TEST_PLAN — FIX-002: titularidad del reporte Excel

## Fuentes

Requisito del usuario de 2026-10-30, PLAN.md, SecurityConfig y el manejo actual de JWT, AuthenticatedUserResolver, InstitutionalScopePort, InstitutionalScopeJpaRepository, consultas de estudiantes/asistencias por grupo, matriz HTTP AS-IS y TESTING_STANDARD. El endpoint no está en el OpenAPI canónico.

## Matriz conductual

| ID | Escenario | Observable | Esperado | Implementación incorrecta detectada | Nivel |
|---|---|---|---|---|---|
| F2-01 | DOCENTE titular | XLSX, cabecera y fila válidas | 200 | denegar todo docente permitido |
| F2-02 | DOCENTE ajeno | status, JSON FORBIDDEN, sin archivo/datos y query nunca invocada | 403 antes de leer | autorización posterior a la consulta o solo ocultar workbook |
| F2-03 | DOCENTE con rol activo pero sin identidad docente resuelta | 403 y query nunca invocada | fallar cerrado | usar Optional.empty como permiso implícito |
| F2-04 | COORDINADOR dentro del programa del grupo | XLSX válido | 200 | exigir titularidad docente a todo rol |
| F2-05 | COORDINADOR fuera de su scope | 403 y query nunca invocada | acceso según scope institucional | acceso global por solo tener rol COORDINADOR |
| F2-06 | ADMINISTRADOR activo | XLSX válido | 200 | retirar acceso institucional administrador |
| F2-07 | ADMINISTRADOR inactivo | 403 y query nunca invocada | fail-closed | confiar solo en rol del JWT |
| F2-08 | DOCENTE y COORDINADOR con roles activos; vía de coordinador válida aunque no haya identidad docente | XLSX válido | 200 por la vía coordinador | negar usuario legítimo multirrol o saltar checks de scope |
| F2-09 | DOCENTE y COORDINADOR, sin identidad docente y sin scope de coordinador | 403, query nunca invocada | ninguna vía válida | bypass por identidad docente ausente |
| F2-10 | ESTUDIANTE | 403 en SecurityConfig; query nunca invocada | prohibido | ampliar RBAC |
| F2-11 | sin Bearer | 401 | no entra al controller/query | aceptar solicitud anónima |
| F2-12 | token inválido | 401 | no entra al controller/query | confiar en JWT no validado |
| F2-13 | usuario ajeno manda usuarioEjecutor/idUsuario del titular en query | 403; se consulta el actor del principal | no hay suplantación | usar ID enviado por cliente |
| F2-14 | directo al controller de formato | DTO de entrada contiene grupo y UUID del resolver; workbook mantiene estructura | misma representación | perder identidad autenticada o cambiar Excel |
| F2-15 | DOCENTE autenticado con identidad docente residual y titularidad true, pero sin roles institucionales activos | Forbidden y `ReporteAsistenciaQueryPort` nunca invocado | 403 antes de leer | conceder por ownership sin rol institucional activo |
| F2-16 | COORDINADOR autenticado con scope residual válido, pero sin roles institucionales activos | Forbidden y `ReporteAsistenciaQueryPort` nunca invocado | 403 antes de leer | conceder por scope sin rol institucional activo |
| F2-17 | fila XLSX con fechas, textos, número de sesión, asistencia y observación nulos | XLSX legible, 11 columnas y valores vacíos/`SIN REGISTRO` | 200 sin excepción | NPE, pérdida de columnas o estado incorrecto |

En usuarios con varios roles se admite una vía propia autorizada por rol institucional activo y su scope. El rol DOCENTE sin identidad no da acceso por sí mismo. El matcher HTTP ya exige una de las autoridades permitidas.

## Efectos laterales

Para toda denegación, verificar que ReporteAsistenciaQueryPort no se invoca y que no se construye respuesta XLSX. El slice usa wiring, interactor, caso de uso, SecurityConfig, decoder/converter, resolver y handler reales; solo los puertos secundarios se simulan.

## Pruebas requeridas y snapshot RED

- ReporteAsistenciaTitularidadHttpTest: F2-02 a F2-05 y F2-08 a F2-13.
- ReporteAsistenciaAdministradorAccessHttpTest: F2-06 y F2-07.
- GenerarReporteAsistenciaUseCaseImplTest: F2-01 a F2-09 y F2-15/F2-16, con asserts de ausencia de query.
- ReporteAsistenciaControllerTest: F2-14/F2-17; conservar sus asserts de formato ya existentes.

F2-15 y F2-16 establecen `findRolesActivosByUsuario` como `Set.of()` y simulan identidad/scope residuales válidos; ambas solicitudes deben rechazarse antes de consultar el reporte. No se crea un caso para respuesta `null`: el puerto devuelve `Set` y no declara `null` como resultado admitido.

F2-17 serializa una fila cuyos once campos de entrada son nulos y vuelve a abrir el cuerpo con `XSSFWorkbook`; exige las once celdas de datos y de cabecera. Fechas, textos y número quedan en blanco; asistencia nula conserva `SIN REGISTRO`.

### RED_SNAPSHOT (antes del cambio productivo)

- Fecha/hora local: 2026-10-30 18:12–18:14 America/Bogota; base de código `e231088217e44b43633e64ba45bd078a7b98b53e`.
- Comando: `.\mvnw.cmd -B -ntp test '-Dtest=ReporteAsistenciaTitularidadHttpTest,ReporteAsistenciaAdministradorAccessHttpTest' '-Djacoco.skip=true'`.
- Resultado real: Maven `BUILD FAILURE` (exit 1 por aserciones esperadas); 12 tests, 8 failures, 0 errors, 0 skipped. La suite compiló y ejecutó ambas clases.
- Fallos observados: DOCENTE ajeno, DOCENTE sin identidad, COORDINADOR ajeno y multirrol sin scope devolvieron 200 en vez de 403; las verificaciones de scope positivo para COORDINADOR y multirrol no tuvieron interacción; ADMINISTRADOR inactivo devolvió 200; las verificaciones de rol/estado de ADMINISTRADOR activo no tuvieron interacción. DOCENTE titular y ADMINISTRADOR activo conservaron 200/XLSX.
- `ReporteAsistenciaTitularidadHttpTest.java` SHA-256: `EFF74D9CC30D4F30E5B34B886D5EB66697E4BAE38538117BB61F6133EDD7428B`.
- `ReporteAsistenciaAdministradorAccessHttpTest.java` SHA-256: `8B6070F49A1A170AF22A2D02520E3EDF0462C5B3AC7141E3665D156447C0217C`.
- Los archivos probados se congelan; este snapshot es la evidencia causal RED de autorización ausente en el AS-IS.

## Integración

`AuthorizationReportJpaParityIT` se ejecuta solo con el perfil Failsafe `integration`, no con `mvn verify`. La inspección de su cobertura actual encontró:

| Capacidad | Cobertura en `AuthorizationReportJpaParityIT` |
|---|---|
| `findRolesActivosByUsuario` | No cubierta |
| `isAdministradorActivo` | No cubierta |
| `canDocenteAccessGrupo` | Cubierta en la matriz de usuarios x grupos |
| `canCoordinadorAccessGrupo` | No cubierta; la prueba compara scope por programa, no por grupo |
| filas del reporte | Cubiertas con comparación JDBC/JPA por grupo, orden, nulos y UTC |

El perfil requiere `application-integration` y `SPRING_DATASOURCE_URL`; la documentación local apunta a la base institucional existente `gestionasistenciadb`. No se identificó una base SQL Server desechable, aislada y autorizada para esta corrida. Por tanto `-Pintegration` queda **NOT_RUN**; no se conecta al contenedor/base originales. La IT observada realiza lecturas de comparación, pero eso no demuestra aislamiento del destino configurado.

`mvn verify` valida suites unitarias/componentes, ArchUnit y JaCoCo. Los tests HTTP usan decoder de prueba y mocks de `InstitutionalScopePort`/`ReporteAsistenciaQueryPort`; no certifican JWT firmado por el IdP ni las capacidades SQL Server no cubiertas arriba.

## Límites de cobertura

JaCoCo local se informa desde el verify recién ejecutado. Sonar, CodeQL y CI remoto son NO EJECUTADOS salvo que el servicio se encuentre disponible sin push.
