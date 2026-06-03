package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.mapper.ConsultarAsistenciasPorGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

/**
 * Interactor del puerto de entrada para consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoInteractor implements ConsultarAsistenciasPorGrupoInputPort {

    private final ConsultarAsistenciasPorGrupoUseCase useCase;

    public ConsultarAsistenciasPorGrupoInteractor(final ConsultarAsistenciasPorGrupoUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso ConsultarAsistenciasPorGrupoUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public List<AsistenciaConsultadaDTO> execute(final ConsultarAsistenciasPorGrupoDTO dto) {
        final ConsultarAsistenciasPorGrupoDomain domain = ConsultarAsistenciasPorGrupoMapper.toDomain(dto);
        final List<AsistenciaConsultadaEntity> resultado = useCase.execute(domain);
        return ConsultarAsistenciasPorGrupoMapper.toDTOs(resultado);
    }
}
