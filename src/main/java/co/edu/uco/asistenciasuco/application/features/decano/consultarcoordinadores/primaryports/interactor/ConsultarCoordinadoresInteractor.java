package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.mapper.ConsultarCoordinadoresMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;

import java.util.Objects;
import java.util.UUID;

/**
 * Interactor para la consulta de coordinadores de la facultad (paginacion y filtros en servidor).
 */
public final class ConsultarCoordinadoresInteractor implements ConsultarCoordinadoresInputPort {

    private final ConsultarCoordinadoresUseCase useCase;

    public ConsultarCoordinadoresInteractor(final ConsultarCoordinadoresUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarCoordinadoresUseCase es obligatorio.");
    }

    @Override
    public PaginaResultado<CoordinadorDTO> execute(final UUID actorUsuarioId, final String busqueda,
                                                   final Boolean estaActivo, final Boolean sinPrograma,
                                                   final Integer pagina, final Integer tamano) {
        final int pag = ConsultaPaginadaRules.pagina(pagina);
        final int tam = ConsultaPaginadaRules.tamano(tamano);
        final String filtro = ConsultaPaginadaRules.busqueda(busqueda);
        final var resultado = useCase.execute(actorUsuarioId, filtro, estaActivo, sinPrograma, pag, tam);
        return new PaginaResultado<>(resultado.elementos().stream().map(ConsultarCoordinadoresMapper::toDTO).toList(),
                resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalElementos());
    }
}
