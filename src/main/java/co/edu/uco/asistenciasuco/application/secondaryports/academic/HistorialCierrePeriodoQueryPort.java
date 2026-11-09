package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CierrePeriodoReporteProjection;

import java.util.List;

/**
 * Puerto secundario para consultar el historial de reportes de cierre masivo de periodo (F-ADM-006).
 */
public interface HistorialCierrePeriodoQueryPort {
    List<CierrePeriodoReporteProjection> consultarHistorialCierres();
}
