package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.DocentesProgramaPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto.ConsultarDocentesProgramaDTO;

import java.util.UUID;

public interface ConsultarDocentesProgramaInputPort {

    DocentesProgramaPaginaDTO execute(UUID actorUsuarioId, ConsultarDocentesProgramaDTO consulta);
}
