package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.Comando;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.ConsultarCalendarioDocenteUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;

import java.util.List;
import java.util.Objects;

/** ATA D7: calendario del docente titular. El perfil y el rango ({@code SEC_001}, {@code CAL_001}) los valida la DB. */
public final class ConsultarCalendarioDocenteUseCaseImpl implements ConsultarCalendarioDocenteUseCase {

    private final SesionCalendarioRepositoryPort repositorio;

    public ConsultarCalendarioDocenteUseCaseImpl(final SesionCalendarioRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "SesionCalendarioRepositoryPort es obligatorio.");
    }

    @Override
    public List<SesionCalendario> execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar el calendario del docente es obligatorio.");
        return CalendarioSesionesMapper.aSesiones(repositorio.consultarDocente(comando.usuarioId(), comando.desde(), comando.hasta()));
    }
}
