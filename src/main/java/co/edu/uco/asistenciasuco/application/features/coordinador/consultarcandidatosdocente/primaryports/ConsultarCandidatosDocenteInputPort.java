package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.dto.DocenteCandidatoDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarCandidatosDocenteInputPort {

    List<DocenteCandidatoDTO> execute(UUID actorUsuarioId, String busqueda);
}
