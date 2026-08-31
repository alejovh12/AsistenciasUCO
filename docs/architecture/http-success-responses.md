# HTTP success responses

Los responses HTTP exitosos que usan envoltorios en el Primary Adapter se representan mediante tipos inmutables y explicitos.

`ApiDataResponse<T>`:

```json
{
  "exitoso": true,
  "datos": {}
}
```

`ApiListResponse<T>`:

```json
{
  "exitoso": true,
  "datos": [],
  "total": 0
}
```

`ApiMessageResponse`:

```json
{
  "exitoso": true,
  "mensaje": "Operacion realizada correctamente."
}
```

Application no conoce estos tipos. Son parte del Primary Adapter y solo envuelven resultados al construir el contrato HTTP.
