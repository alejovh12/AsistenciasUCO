package co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.domain.ActualizarGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.entity.ActualizarGrupoResultadoEntity;

public interface ActualizarGrupoUseCase {

    ActualizarGrupoResultadoEntity execute(ActualizarGrupoDomain domain);
}
