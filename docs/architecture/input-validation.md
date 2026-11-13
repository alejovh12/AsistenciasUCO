---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-12
---

# Validacion de entrada

Flujo HTTP:

HTTP
-> Request / PathVariable / RequestParam
-> Jakarta Bean Validation (@Valid en el @RequestBody; primera barrera)
-> RequestValidator propio
-> ValidationResult
-> RequestValidationGuard
-> Mapper HTTP
-> Application

## Bean Validation (ROL-06, decision G de John, 2026-11-12)

Todo `@RequestBody` con un request tipado lleva `@Valid`. Las anotaciones (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`,
`@Positive`, `@Min`, `@Max`, `@Valid` en cascada) viven SOLO en los requests de `infrastructure/adapter/primary/controller/**`;
Application y Domain no dependen de `jakarta.validation` (ArchUnit). Reglas:

- Bean Validation es la primera barrera y replica las reglas simples por campo del validator propio (obligatoriedad,
  longitud maxima de la columna, formato). Nunca debe rechazar algo que el validator propio acepta: los patrones de
  `BeanValidationPatterns` toleran espacios alrededor y la cadena vacia en campos opcionales.
- Los validators propios se conservan para las reglas condicionales o entre campos (modos excluyentes, documento y tipo
  juntos, alias legados) y siguen siendo la autoridad de esas reglas.
- `MethodArgumentNotValidException` responde con el mismo contrato que `RequestValidationException`: 400
  `VALIDATION_ERROR` con `details[]` (`field`, `code`, `message`). `BeanValidationFieldErrorMapper` deja un error por
  campo (obligatorio, formato, longitud, rango, valor), en el orden de declaracion del request, y solo expone el `message`
  explicito de la anotacion: el texto por defecto del proveedor nunca sale.
- Cuando el validator propio usa un codigo distinto del que corresponde por defecto a la anotacion, el request lo fija con
  `payload = BeanValidationCodes.<Codigo>.class`.
- Se deja a Application, a proposito, lo que tiene codigo de negocio propio (por ejemplo el modo vacio al abrir una toma,
  `TOM_002`, o el motivo ausente del retiro de estudiante).
- Cuerpos sin request tipado: `SesionV2Controller` (`Map`, lo valida `SesionV2RequestParser`), el webhook de Event Grid
  (`List<Map>`, contrato de Azure) y `RealtimeEventsController` (`Map`).

No se utilizan regex dispersas. Los patrones reutilizables se centralizan en ValidationPatterns. ValidationHelper expone primitivas reutilizables para formato, longitud, UUID vacio, rangos simples y normalizacion.

Cada request HTTP tiene un validator propio. Los validators acumulan varios errores en ValidationResult y RequestValidationGuard convierte el resultado invalido en RequestValidationException en la frontera HTTP.

Los mensajes raw de Spring, Jackson, SQL o excepciones tecnicas no se exponen al cliente. Los errores de entrada usan mensajes controlados y, cuando aplica, details con field, code y message.

## Contrato JSON estricto

- Las propiedades desconocidas se rechazan.
- Las propiedades duplicadas se rechazan.
- El contenido adicional despues del objeto JSON esperado se rechaza.
- Las coerciones automaticas de tipos se rechazan.
- Los parametros booleanos HTTP aceptan unicamente `true` o `false`.
- Los mensajes internos de Jackson y Spring no forman parte del contrato publico.

Los booleanos de `RequestParam` se enlazan mediante `WebDataBinder` y `StrictBooleanPropertyEditor`, con representacion exacta `true`/`false`.

Ejemplo:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Hay campos con informacion invalida. Revise los datos enviados.",
  "details": [
    {
      "field": "primerNombre",
      "code": "FIELD_INVALID_FORMAT",
      "message": "Solo se permiten letras, espacios, apostrofes y guiones."
    }
  ],
  "correlationId": "..."
}
```

Las invariantes importantes permanecen en application mediante rules puras. Las reglas de negocio viven en use case/domain y la base de datos sigue siendo la ultima barrera de integridad.
