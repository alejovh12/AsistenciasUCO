package co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;

import java.util.List;
import java.util.UUID;

public interface ConsultarGruposDecanoUseCase {

    List<GrupoEntity> execute(UUID actorUsuarioId);
}
