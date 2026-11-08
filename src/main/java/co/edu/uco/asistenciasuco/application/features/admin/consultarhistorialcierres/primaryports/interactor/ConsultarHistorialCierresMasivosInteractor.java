package co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.ConsultarHistorialCierresMasivosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.mapper.ConsultarHistorialCierresMasivosMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.ConsultarHistorialCierresMasivosUseCase;

import java.util.List;
import java.util.Objects;

/**
 * Interactor para consultar el historial de reportes de cierre masivo (F-ADM-006).
 */
public final class ConsultarHistorialCierresMasivosInteractor implements ConsultarHistorialCierresMasivosInputPort {

    private final ConsultarHistorialCierresMasivosUseCase useCase;

    public ConsultarHistorialCierresMasivosInteractor(final ConsultarHistorialCierresMasivosUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarHistorialCierresMasivosUseCase es obligatorio.");
    }

    @Override
    public List<CierrePeriodoReporteDTO> execute() {
        return useCase.execute().stream()
                .map(ConsultarHistorialCierresMasivosMapper::toDTO)
                .toList();
    }
}
