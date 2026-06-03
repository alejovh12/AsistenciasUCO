package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.List;

/**
 * Caso de uso para consultar asistencias por grupo.
 */
public interface ConsultarAsistenciasPorGrupoUseCase
        extends UseCaseWithReturn<ConsultarAsistenciasPorGrupoDomain, List<AsistenciaConsultadaEntity>> {
}
