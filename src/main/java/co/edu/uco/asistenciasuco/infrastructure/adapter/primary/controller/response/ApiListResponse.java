package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.util.List;
import java.util.Objects;

public record ApiListResponse<T>(
        boolean exitoso,
        List<T> datos,
        int total
) {

    public ApiListResponse {
        datos = List.copyOf(Objects.requireNonNull(datos, "Los datos de la respuesta HTTP son obligatorios."));
    }
}
