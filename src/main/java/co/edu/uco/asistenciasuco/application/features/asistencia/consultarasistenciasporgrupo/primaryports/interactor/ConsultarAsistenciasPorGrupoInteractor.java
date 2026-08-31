package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.mapper.ConsultarAsistenciasPorGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;

import java.util.List;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para consultar asistencias por grupo.
 */
public final class ConsultarAsistenciasPorGrupoInteractor implements ConsultarAsistenciasPorGrupoInputPort {

    private final ConsultarAsistenciasPorGrupoUseCase useCase;

    public ConsultarAsistenciasPorGrupoInteractor(final ConsultarAsistenciasPorGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarAsistenciasPorGrupoUseCase es obligatorio.");
    }

    @Override
    public List<AsistenciaConsultadaDTO> execute(final ConsultarAsistenciasPorGrupoDTO dto) {
        final ConsultarAsistenciasPorGrupoDomain domain = ConsultarAsistenciasPorGrupoMapper.toDomain(dto);
        final List<AsistenciaConsultadaEntity> resultado = useCase.execute(domain);
        return ConsultarAsistenciasPorGrupoMapper.toDTOs(resultado);
    }
}