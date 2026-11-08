package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto;

/** Vinculo asignatura-plan: una fila por (asignatura, plan); compartida = la asignatura esta en mas de un programa. */
public record AsignaturaPlanDTO(java.util.UUID idAsignatura, String codigo, String nombre, Integer credito, java.util.UUID idArea, String nombreArea,
        java.util.UUID idComponente, String nombreComponente, java.util.UUID idPlanEstudio, Integer inpPlanEstudio, java.util.UUID idPrograma,
        String nombrePrograma, java.util.UUID idSemestrePlanEstudio, Integer numeroSemestre, String codigoSemestre, String codigoCaracter,
        String nombreCaracter, boolean estaActivoVinculo, boolean estaActivaAsignatura, boolean compartida) {
}
