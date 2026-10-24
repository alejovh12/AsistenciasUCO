package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

public record PaginaBitacoraProjection(List<BitacoraSesionProjection> sesiones, int totalRegistros) {

    public PaginaBitacoraProjection {
        sesiones = List.copyOf(sesiones);
    }
}
