package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain.CierrePeriodoReporteDomain;

import java.util.List;

/**
 * Caso de uso para consultar el historial de cierres masivos de periodo (F-ADM-006).
 */
public interface ConsultarHistorialCierresMasivosUseCase {
    List<CierrePeriodoReporteDomain> execute();
}
