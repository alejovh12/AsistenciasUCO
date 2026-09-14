package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto.InstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.mapper.ConsultarInstitucionesMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.ConsultarInstitucionesUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarInstitucionesInteractor implements ConsultarInstitucionesInputPort {
    private final ConsultarInstitucionesUseCase useCase;
    public ConsultarInstitucionesInteractor(final ConsultarInstitucionesUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<InstitucionDTO> execute() {
        return useCase.execute().stream().map(ConsultarInstitucionesMapper::toDTO).toList();
    }
}
