package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;

import java.util.List;

/**
 * F-ADM-006: Puerto primario para consultar el historial de cierres masivos semestrales.
 */
public interface ConsultarHistorialCierresMasivosInputPort {
    List<CierrePeriodoReporteDTO> execute();
}
