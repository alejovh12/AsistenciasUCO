package co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.domain.CrearGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.entity.CrearGrupoResultadoEntity;

public interface CrearGrupoUseCase {

    CrearGrupoResultadoEntity execute(CrearGrupoDomain domain);
}
