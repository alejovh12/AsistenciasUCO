package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

/** Pagina de planes de estudio con totales por la tabla puente. */
public record PaginaPlanesEstudioProjection(java.util.List<PlanEstudioProjection> elementos, long total) {
}
