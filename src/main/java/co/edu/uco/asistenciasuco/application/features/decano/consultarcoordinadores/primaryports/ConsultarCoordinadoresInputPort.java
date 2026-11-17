package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;

import java.util.UUID;

/**
 * Puerto primario para la consulta de coordinadores del decano (CIERRE-W1B: paginacion y filtros en servidor).
 */
public interface ConsultarCoordinadoresInputPort {

    PaginaResultado<CoordinadorDTO> execute(UUID actorUsuarioId, String busqueda, Boolean estaActivo,
                                            Boolean sinPrograma, Integer pagina, Integer tamano);
}
