---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-09
---

# Validación — BE-FASE-20261109

Entorno: Windows 11, Java 25 (`JAVA_HOME`), wrapper Maven del repo, SQL Server local `sql_server_asistencias` (BD `gestionasistenciadb`, solo lectura), 2026-11-09. Base `develop` = `f26c5e93`.

| Rama | Commits | Comando | Resultado |
|---|---|---|---|
| `fix/sonar-reliability-notificacion-beans` | `480c385e` | `mvnw -q test -Dtest=NotificacionBeansDesambiguacionTest` (antes del cambio) | RED: 1 fallo con las 3 dependencias ambiguas |
| idem | idem | `mvnw -q test -Dtest=NotificacionBeansDesambiguacionTest,AdminNotificacionAdapterConfigurationTest,SmtpNotificationWiringTest,NotificationWiringTest` | GREEN, exit 0 |
| idem | idem | `verify` global | `NOT_RUN` localmente; lo ejecuta Backend CI |
| `fix/estudiante-sesiones-materia-acotadas` | `a7639841`, `944b7171` | `mvnw -q test -Dtest=ConsultarSesionesMateriaEstudianteAcotadoTest` (stubs RED) | RED: 4/4 fallan por la causa esperada |
| idem | idem | pruebas focalizadas (caso de uso, adaptador JPA, contratos JPA, ArchUnit) | GREEN, exit 0 (77 ejecutadas; 1 falla inicial por una verificación antigua del puerto de 2 argumentos, corregida) |
| idem | idem | `mvnw -Pintegration -Dit.test=SesionesEstudianteAcotadasSqlServerIT,AsistenciaSesionEstudianteJpaParityIT verify` (SQL Server real, solo lectura) | `SesionesEstudianteAcotadasSqlServerIT` 4/4 PASS, 0 skipped; `AsistenciaSesionEstudianteJpaParityIT` 3 ejecutadas, 0 fallos, 1 skipped (preexistente). exit 0 |
| idem | idem | `mvnw -B -ntp verify` | 3609 ejecutadas, 0 fallos, 0 errores, 0 omitidas; `jacoco:check` "All coverage checks have been met"; BUILD SUCCESS; exit 0 |

Limitaciones:
- La IT usa datos vivos y no escribe; no se levantó una base alternativa ni se sembraron datos.
- No se midió el número de sentencias SQL ni el tiempo; la acotación se demuestra por filas recuperadas (menos que la lectura completa) y por la forma del HQL (`in :idsSesion`, sin `idGrupo`).
- Validación HTTP real, despliegue en 8080, CI remoto y SonarCloud: `NOT_RUN` (ver REPORTE.md §1 y §6). Ningún endpoint se certifica por HTTP real en esta fase.
- Los SHA de las ramas no han sido publicados.
