package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;

import java.util.List;
import java.util.UUID;

/**
 * Puerto primario para la consulta de coordinadores del decano (F-DEC-004: paginacion con ApiPageResponse).
 */
public interface ConsultarCoordinadoresInputPort {
    List<CoordinadorDTO> execute(UUID actorUsuarioId);

    PaginaResultado<CoordinadorDTO> execute(UUID actorUsuarioId, Integer pagina, Integer tamano);
}
