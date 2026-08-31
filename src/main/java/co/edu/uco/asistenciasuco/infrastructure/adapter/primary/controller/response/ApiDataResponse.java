package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

public record ApiDataResponse<T>(
        boolean exitoso,
        T datos
) {
}
