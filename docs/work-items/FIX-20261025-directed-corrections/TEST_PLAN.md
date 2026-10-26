# Test plan — FIX-20261025-directed-corrections

## RED

1. Enviar `PATCH /api/v1/decano/coordinadores/{id}/estado` con cuerpo JSON
   `null`; debe responder 400 y no invocar el caso de uso.
2. Construir la configuración de activación UTC V2 sin la propiedad; no debe
   instalar el interceptor que bloquea `/api/v2/sesiones/**`.

3. Arrancar con `app.sesiones.v2.enabled` vacío o inválido (`""`, `tru`, `yes`,
   `1`); el contexto debe fallar nombrando la propiedad, sin dejar V2 accesible
   sin verificar el contrato SQL UTC-D06. Ausente/`true` instala solo el
   verificador SQL; `false` instala solo el bloqueo 404.

## GREEN

- Ejecutar las pruebas focales de `DecanoCoordinadorGestionControllerTest` y
  `SesionUtcActivationGuardTest`.

## Validación complementaria

- Ejecutar `verify` si el entorno local permite los gates del proyecto.
- Reportar por separado cualquier fallo de DB/integración o check remoto no
  ejecutado; no convertir un skip en PASS.
