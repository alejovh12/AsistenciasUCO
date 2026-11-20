package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.EstudianteSemestreCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarSemestreEstudianteCommand;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.ActualizarSemestreEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ActualizarSemestreEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ResultadoSemestreEstudianteDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * El coordinador registra el semestre que cursa un estudiante en un programa que coordina. El ambito (titularidad del programa,
 * vinculo del estudiante y rango del catalogo de semestres) lo fuerza usp_actualizar_semestre_estudiante_programa.
 */
public final class ActualizarSemestreEstudianteUseCaseImpl implements ActualizarSemestreEstudianteUseCase {

    private final EstudianteSemestreCommandPort commandPort;

    public ActualizarSemestreEstudianteUseCaseImpl(final EstudianteSemestreCommandPort commandPort) {
        this.commandPort = Objects.requireNonNull(commandPort, "EstudianteSemestreCommandPort es obligatorio.");
    }

    @Override
    public ResultadoSemestreEstudianteDomain execute(final ActualizarSemestreEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain) || ObjectHelper.isNull(domain.idEstudiante()) || ObjectHelper.isNull(domain.idPrograma())) {
            throw new CrosscuttingException("El estudiante y el programa son obligatorios para actualizar el semestre.");
        }
        final String mensaje = commandPort.actualizarSemestre(new ActualizarSemestreEstudianteCommand(
                domain.idEstudiante(), domain.idPrograma(), domain.semestreActual(), domain.actorUsuarioId()));
        return new ResultadoSemestreEstudianteDomain(mensaje);
    }
}
