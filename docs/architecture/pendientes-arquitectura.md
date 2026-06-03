# Pendientes de arquitectura

## Presenters pendientes

Un presenter es un componente de salida del caso de uso que transforma el resultado interno (modelo de aplicacion o respuesta de dominio) al formato especifico del canal de entrega (REST, WebSocket, etc.).

En esta arquitectura podria servir para:
- desacoplar completamente la orquestacion de `application/usecase/impl` de cualquier detalle de presentacion;
- centralizar el mapeo de respuestas exitosas y errores hacia el contrato HTTP;
- facilitar pruebas por separado del flujo de negocio y del formato de respuesta.

Si se implementa mas adelante, la ubicacion sugerida es:
- contrato en `application/ports/output/RegistrarEstudianteEnGrupoPresenter.java`;
- implementacion REST en `infrastructure/adapter/primary/rest/presenters/RegistrarEstudianteEnGrupoRestPresenter.java`.

Por ahora no se implementa para no sobrecomplicar el boceto actual. La presentacion de respuestas se mantiene con DTOs, `ApiResponse` y `GlobalExceptionHandler`.
