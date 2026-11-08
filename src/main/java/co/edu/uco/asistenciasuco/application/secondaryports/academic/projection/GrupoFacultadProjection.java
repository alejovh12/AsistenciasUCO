package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/** Grupo de la facultad del decano con los nombres de sus programas. */
public record GrupoFacultadProjection(java.util.UUID idGrupo, Integer codigo, String nombre, java.util.UUID idPeriodoAcademico, java.util.UUID idAsignatura, String nombreAsignatura,
        java.util.UUID idDocente, Integer capacidadMaximaPermitida, Integer estudiantesActivos, Integer cuposDisponibles, boolean grupoEstaHablitado,
        java.util.List<String> programas) {
}
