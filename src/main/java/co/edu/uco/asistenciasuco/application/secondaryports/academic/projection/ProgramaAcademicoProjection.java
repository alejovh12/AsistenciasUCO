package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Fila de programa academico leida de {@code uv_programa} / {@code usp_consultar_programas_academicos}. */
public record ProgramaAcademicoProjection(
        UUID id,
        String nombre,
        UUID idTipoPrograma,
        String nombreTipoPrograma,
        UUID idModalidad,
        String nombreModalidad,
        UUID idJornada,
        String nombreJornada,
        UUID idCoordinador,
        String nombreCoordinador,
        UUID idFacultad,
        String nombreFacultad,
        boolean estaActivo
) {
}
