package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;

import java.util.UUID;

public interface ConsultarCoordinadoresUseCase {

    /** El ambito (facultad del decano) lo valida el procedimiento almacenado con el usuario ejecutor. */
    PaginaResultado<CoordinadorDomain> execute(UUID actorUsuarioId, String busqueda, Boolean estaActivo,
                                               Boolean sinPrograma, int pagina, int tamano);
}
