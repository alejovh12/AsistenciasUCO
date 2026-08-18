package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithoutInputWithReturn;

import java.util.List;

/**
 * Caso de uso para consultar grupos.
 */
public interface ConsultarGruposUseCase extends UseCaseWithoutInputWithReturn<List<GrupoEntity>> {
}
