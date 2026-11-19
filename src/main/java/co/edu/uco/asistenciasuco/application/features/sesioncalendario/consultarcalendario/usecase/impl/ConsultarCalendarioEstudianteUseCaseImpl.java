package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.Comando;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.ConsultarCalendarioEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;

import java.util.List;
import java.util.Objects;

/** ATA D7: calendario del estudiante. El perfil y el rango ({@code SEC_001}, {@code CAL_001}) los valida la DB. */
public final class ConsultarCalendarioEstudianteUseCaseImpl implements ConsultarCalendarioEstudianteUseCase {

    private final SesionCalendarioRepositoryPort repositorio;

    public ConsultarCalendarioEstudianteUseCaseImpl(final SesionCalendarioRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "SesionCalendarioRepositoryPort es obligatorio.");
    }

    @Override
    public List<SesionCalendario> execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar el calendario del estudiante es obligatorio.");
        return CalendarioSesionesMapper.aSesiones(repositorio.consultarEstudiante(comando.usuarioId(), comando.desde(), comando.hasta()));
    }
}
