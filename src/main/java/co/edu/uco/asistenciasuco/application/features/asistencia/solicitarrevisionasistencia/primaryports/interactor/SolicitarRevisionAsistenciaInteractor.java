package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.mapper.SolicitarRevisionAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaInteractor implements SolicitarRevisionAsistenciaInputPort {

    private final SolicitarRevisionAsistenciaUseCase useCase;

    public SolicitarRevisionAsistenciaInteractor(final SolicitarRevisionAsistenciaUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso SolicitarRevisionAsistenciaUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public void execute(final SolicitarRevisionAsistenciaDTO dto) {
        final SolicitarRevisionAsistenciaDomain domain = SolicitarRevisionAsistenciaMapper.toDomain(dto);
        useCase.execute(domain);
    }
}
