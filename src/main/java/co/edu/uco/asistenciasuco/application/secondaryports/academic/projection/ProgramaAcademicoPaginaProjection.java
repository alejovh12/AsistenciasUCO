package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

/** Pagina de programas con el total de registros que cumplen el filtro. */
public record ProgramaAcademicoPaginaProjection(List<ProgramaAcademicoProjection> programas, int totalRegistros) {

    public ProgramaAcademicoPaginaProjection {
        programas = List.copyOf(programas);
    }
}
