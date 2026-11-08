package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto;

/** Solicitud de asociar una asignatura existente a un plan (CULM-001 B2NM). */
public record AsociarAsignaturaPlanDTO(java.util.UUID idAsignatura, Integer semestreNumero, String codigoCaracter) {
}
