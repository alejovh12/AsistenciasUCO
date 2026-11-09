package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto;

/** Programa vigente de un grupo (selector de matricula manual y de ventana QR). */
public record ProgramaResumenDTO(java.util.UUID idPrograma, String nombrePrograma, java.util.UUID idPlanEstudio, Integer inpPlanEstudio) {
}
