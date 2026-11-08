package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.List;

public record PaginaPeriodosAdminProjection(List<PeriodoAdminProjection> elementos, long total) {
}
