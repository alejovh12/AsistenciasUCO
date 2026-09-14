package co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto.FacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.mapper.ConsultarFacultadesMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.ConsultarFacultadesUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarFacultadesInteractor implements ConsultarFacultadesInputPort {
    private final ConsultarFacultadesUseCase useCase;
    public ConsultarFacultadesInteractor(final ConsultarFacultadesUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<FacultadDTO> execute() {
        return useCase.execute().stream().map(ConsultarFacultadesMapper::toDTO).toList();
    }
}
