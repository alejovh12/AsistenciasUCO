package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

public record PaginaAlertasProjection(List<AlertaTempranaProjection> alertas, int totalRegistros) {

    public PaginaAlertasProjection {
        alertas = List.copyOf(alertas);
    }
}
