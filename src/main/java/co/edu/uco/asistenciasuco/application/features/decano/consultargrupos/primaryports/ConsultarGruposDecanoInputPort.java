package co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarGruposDecanoInputPort {

    List<GrupoDTO> execute(UUID actorUsuarioId);
}
