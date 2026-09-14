package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;

import java.util.List;
import java.util.UUID;

public interface ConsultarCoordinadoresInputPort { List<CoordinadorDTO> execute(UUID actorUsuarioId); }
