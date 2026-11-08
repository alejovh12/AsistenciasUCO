package co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto;

/** Asignatura activa de la institucion sin vinculo activo en el plan; planes = vinculos activos en otros planes. */
public record AsignaturaAsociableDTO(java.util.UUID idAsignatura, String codigo, String nombre, Integer credito,
                                     String nombreArea, String nombreComponente, String planes) {
}
