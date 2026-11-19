package co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.FaltaReclamable;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.usecase.ConsultarFaltasReclamablesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.FilaFaltaReclamable;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort.PaginaFaltasReclamables;

import java.util.Objects;

/** ATA D10: consulta paginada de las faltas por reclamar del estudiante. El perfil y la pagina ({@code SEC_001}, {@code PAG_001}) los valida la DB. */
public final class ConsultarFaltasReclamablesUseCaseImpl implements ConsultarFaltasReclamablesUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 20;

    private final FaltaReclamableRepositoryPort repositorio;

    public ConsultarFaltasReclamablesUseCaseImpl(final FaltaReclamableRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "FaltaReclamableRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar las faltas reclamables es obligatorio.");
        final int pagina = comando.pagina() == null ? PAGINA_POR_DEFECTO : comando.pagina();
        final int tamano = comando.tamano() == null ? TAMANO_POR_DEFECTO : comando.tamano();
        final PaginaFaltasReclamables datos = repositorio.consultarEstudiante(comando.usuarioId(), pagina, tamano);
        return new Resultado(datos.filas().stream().map(ConsultarFaltasReclamablesUseCaseImpl::aFalta).toList(), pagina, tamano, datos.total());
    }

    private static FaltaReclamable aFalta(final FilaFaltaReclamable f) {
        return new FaltaReclamable(f.idGrupo(), f.nombreGrupo(), f.idAsignatura(), f.nombreAsignatura(), f.idSesion(), f.numeroSesion(),
                f.fechaHoraInicioUtc(), f.fechaHoraFinUtc(), f.idToma(), f.numeroToma(), f.fechaPuntoControlUtc(), f.fechaLimiteReclamoUtc());
    }
}
