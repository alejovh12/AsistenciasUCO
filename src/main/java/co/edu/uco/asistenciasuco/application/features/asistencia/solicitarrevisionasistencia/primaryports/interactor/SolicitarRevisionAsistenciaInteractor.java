package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.mapper.SolicitarRevisionAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaInteractor implements SolicitarRevisionAsistenciaInputPort {

    private final SolicitarRevisionAsistenciaUseCase useCase;

    public SolicitarRevisionAsistenciaInteractor(final SolicitarRevisionAsistenciaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso SolicitarRevisionAsistenciaUseCase es obligatorio.");
    }

    @Override
    public void execute(final SolicitarRevisionAsistenciaDTO dto) {
        final SolicitarRevisionAsistenciaDomain domain = SolicitarRevisionAsistenciaMapper.toDomain(dto);
        useCase.execute(domain);
    }
}