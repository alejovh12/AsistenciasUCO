package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto;

/** Grupo de la facultad del decano; programas = nombres de los programas del grupo en su facultad. */
public record GrupoFacultadDTO(java.util.UUID idGrupo, Integer codigo, String nombre, java.util.UUID idPeriodoAcademico, java.util.UUID idAsignatura, String nombreAsignatura,
        java.util.UUID idDocente, Integer capacidadMaximaPermitida, Integer estudiantesActivos, Integer cuposDisponibles, boolean grupoEstaHablitado,
        java.util.List<String> programas) {
}
