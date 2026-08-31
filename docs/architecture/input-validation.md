# Validacion de entrada

Flujo HTTP:

HTTP
-> Request / PathVariable / RequestParam
-> RequestValidator propio
-> ValidationResult
-> RequestValidationGuard
-> Mapper HTTP
-> Application

El proyecto no utiliza Jakarta Validation como mecanismo de validacion de entrada.

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
