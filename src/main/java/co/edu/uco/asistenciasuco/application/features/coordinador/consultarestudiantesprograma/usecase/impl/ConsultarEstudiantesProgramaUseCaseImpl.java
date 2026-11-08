package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.ConsultarEstudiantesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.ConsultarEstudiantesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.PaginaEstudiantesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.mapper.ConsultarEstudiantesProgramaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteProgramaQueryPort;
import java.util.Objects;

/**
 * Estudiantes de los programas del coordinador por usp_consultar_directorio_estudiantes_paginado (CULM-001 B2NM, O2-B2): el ambito (varios
 * programas, R6, COO_003) vive en el SP; ya no se resuelve un unico programa en Java.
 */
public final class ConsultarEstudiantesProgramaUseCaseImpl implements ConsultarEstudiantesProgramaUseCase {

    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;
    private final EstudianteProgramaQueryPort queryPort;

    public ConsultarEstudiantesProgramaUseCaseImpl(final EstudianteProgramaQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    @Override
    public PaginaEstudiantesProgramaDomain execute(final ConsultarEstudiantesProgramaDomain c) {
        Objects.requireNonNull(c, "La consulta de estudiantes es obligatoria.");
        final int pagina = c.pagina() == null || c.pagina() < 1 ? 1 : c.pagina();
        final int tamano = c.tamano() == null || c.tamano() < 1 ? TAMANO_POR_DEFECTO : Math.min(c.tamano(), TAMANO_MAXIMO);
        final String busqueda = c.busqueda() == null || c.busqueda().isBlank() ? null : c.busqueda().strip();
        final var resultado = queryPort.consultarDirectorioPrograma(c.actorUsuarioId(), c.programaId(), busqueda, c.estaActivo(), pagina, tamano);
        return new PaginaEstudiantesProgramaDomain(resultado.elementos().stream().map(ConsultarEstudiantesProgramaRepositoryMapper::toDomain).toList(), resultado.total());
    }
}
