# Corrección posterior: resumen de docentes

Fecha: 2026-10-28

## Causa

El correlation ID `ceca677c-b7ad-4bec-828c-bc659e70fc95` mostró:

```text
java.lang.ClassCastException: java.lang.String cannot be cast to java.util.UUID
DocenteProgramaReporteJpaRepository.java:100
```

La base devolvía correctamente `idTipoVinculacion`, pero el adaptador JPA asumía que el driver siempre entregaría `UUID`. En esta consulta nativa SQL Server lo entrega como `String`.

## Corrección

`DocenteProgramaReporteJpaRepository` ahora acepta `UUID` y `String` para esa columna y convierte el valor mediante `UUID.fromString`. No se modificó el procedimiento, la base, datos académicos, permisos ni identidades.

Se añadió una regresión que reproduce el tipo real entregado por SQL Server.

## Validación

- `DocenteProgramaReporteJpaRepositoryTest`: 11 pruebas, 0 fallos, 0 errores.
- `mvnw.cmd -B -DskipTests package`: `BUILD SUCCESS`.
- Backend reiniciado con el jar corregido en el puerto 8080.
- `GET /actuator/health`: `200`, `UP`.

La respuesta HTTP autenticada del endpoint debe repetirse con la sesión legítima de Jorge para cerrar la validación E2E; no se sustituyó su identidad ni se almacenaron credenciales.
