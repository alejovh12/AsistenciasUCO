package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

public record ApiMessageResponse(
        boolean exitoso,
        String mensaje
) {
}
