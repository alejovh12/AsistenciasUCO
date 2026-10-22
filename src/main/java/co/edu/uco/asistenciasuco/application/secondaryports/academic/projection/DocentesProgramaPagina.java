package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

public record DocentesProgramaPagina(List<DocenteProgramaProjection> elementos, long total) {

    public DocentesProgramaPagina {
        elementos = elementos == null ? List.of() : List.copyOf(elementos);
    }
}
