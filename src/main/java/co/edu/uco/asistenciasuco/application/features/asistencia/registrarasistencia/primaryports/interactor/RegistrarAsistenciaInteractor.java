package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.RegistrarAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.mapper.RegistrarAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.RegistrarAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para registrar asistencia.
 */
public final class RegistrarAsistenciaInteractor implements RegistrarAsistenciaInputPort {

    private final RegistrarAsistenciaUseCase useCase;

    public RegistrarAsistenciaInteractor(final RegistrarAsistenciaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso RegistrarAsistenciaUseCase es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciaDTO dto) {
        final RegistrarAsistenciaDomain domain = RegistrarAsistenciaMapper.toDomain(dto);
        useCase.execute(domain);
    }
}