package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.List;

/**
 * Caso de uso para consultar las sesiones de un grupo.
 */
public interface ConsultarSesionesPorGrupoUseCase
        extends UseCaseWithReturn<ConsultarSesionesPorGrupoDomain, List<SesionConsultadaEntity>> {
}
