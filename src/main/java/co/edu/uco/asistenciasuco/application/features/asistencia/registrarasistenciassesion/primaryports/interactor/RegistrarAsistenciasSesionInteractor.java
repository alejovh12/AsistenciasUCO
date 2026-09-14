package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.mapper.RegistrarAsistenciasSesionMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;

import java.util.Objects;

public final class RegistrarAsistenciasSesionInteractor implements RegistrarAsistenciasSesionInputPort {

    private final RegistrarAsistenciasSesionUseCase useCase;

    public RegistrarAsistenciasSesionInteractor(final RegistrarAsistenciasSesionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "RegistrarAsistenciasSesionUseCase es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciasSesionDTO dto) {
        useCase.execute(RegistrarAsistenciasSesionMapper.toDomain(dto));
    }
}
