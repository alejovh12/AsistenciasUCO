package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/** Asignatura asociable a un plan. */
public record AsignaturaAsociableProjection(java.util.UUID idAsignatura, String codigo, String nombre, Integer credito,
                                            String nombreArea, String nombreComponente, String planes) {
}
