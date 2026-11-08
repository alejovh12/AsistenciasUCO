package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.dto.CoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.mapper.ConsultarCoordinadoresMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Interactor para la consulta de coordinadores de la facultad (F-DEC-004: paginacion en servidor).
 */
public final class ConsultarCoordinadoresInteractor implements ConsultarCoordinadoresInputPort {

    private final ConsultarCoordinadoresUseCase useCase;

    public ConsultarCoordinadoresInteractor(final ConsultarCoordinadoresUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarCoordinadoresUseCase es obligatorio.");
    }

    @Override
    public List<CoordinadorDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarCoordinadoresMapper::toDTO).toList();
    }

    @Override
    public PaginaResultado<CoordinadorDTO> execute(final UUID actorUsuarioId, final Integer pagina, final Integer tamano) {
        final int pag = ConsultaPaginadaRules.pagina(pagina);
        final int tam = ConsultaPaginadaRules.tamano(tamano);
        final List<CoordinadorDTO> todos = execute(actorUsuarioId);
        final int total = todos.size();
        final int offset = (pag - 1) * tam;
        final List<CoordinadorDTO> paginados;
        if (offset >= total) {
            paginados = List.of();
        } else {
            paginados = todos.subList(offset, Math.min(offset + tam, total));
        }
        return new PaginaResultado<>(paginados, pag, tam, total);
    }
}
