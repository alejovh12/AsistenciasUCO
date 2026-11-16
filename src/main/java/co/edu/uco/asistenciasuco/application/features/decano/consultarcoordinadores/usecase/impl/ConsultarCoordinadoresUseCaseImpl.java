package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.mapper.ConsultarCoordinadoresRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;

import java.util.Objects;
import java.util.UUID;

/**
 * CIERRE-W1B: lista paginada de coordinadores de la facultad del decano. La paginacion, los filtros y el ambito se
 * resuelven en {@code usp_consultar_coordinadores_facultad_paginado}; aqui solo se orquesta la consulta.
 */
public final class ConsultarCoordinadoresUseCaseImpl implements ConsultarCoordinadoresUseCase {

    private final CoordinadorQueryPort coordinadorQueryPort;

    public ConsultarCoordinadoresUseCaseImpl(final CoordinadorQueryPort coordinadorQueryPort) {
        this.coordinadorQueryPort = Objects.requireNonNull(coordinadorQueryPort);
    }

    @Override
    public PaginaResultado<CoordinadorDomain> execute(final UUID actorUsuarioId, final String busqueda,
                                                      final Boolean estaActivo, final Boolean sinPrograma,
                                                      final int pagina, final int tamano) {
        final var filas = coordinadorQueryPort.consultarCoordinadoresFacultad(
                Objects.requireNonNull(actorUsuarioId, "El usuario ejecutor es obligatorio."),
                busqueda, estaActivo, sinPrograma, pagina, tamano);
        return new PaginaResultado<>(
                filas.elementos().stream().map(ConsultarCoordinadoresRepositoryMapper::toDomain).toList(),
                pagina, tamano, filas.total());
    }
}
