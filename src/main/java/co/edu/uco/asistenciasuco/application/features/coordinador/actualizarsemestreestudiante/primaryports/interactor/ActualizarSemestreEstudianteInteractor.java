package co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.ActualizarSemestreEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ActualizarSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.primaryports.dto.ResultadoSemestreEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.ActualizarSemestreEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizarsemestreestudiante.usecase.domain.ActualizarSemestreEstudianteDomain;

import java.util.Objects;

public final class ActualizarSemestreEstudianteInteractor implements ActualizarSemestreEstudianteInputPort {

    private final ActualizarSemestreEstudianteUseCase useCase;

    public ActualizarSemestreEstudianteInteractor(final ActualizarSemestreEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ResultadoSemestreEstudianteDTO execute(final ActualizarSemestreEstudianteDTO dto) {
        final String mensaje = useCase.execute(new ActualizarSemestreEstudianteDomain(
                dto.actorUsuarioId(), dto.idEstudiante(), dto.idPrograma(), dto.semestreActual())).mensajeUsuario();
        return new ResultadoSemestreEstudianteDTO(mensaje);
    }
}
