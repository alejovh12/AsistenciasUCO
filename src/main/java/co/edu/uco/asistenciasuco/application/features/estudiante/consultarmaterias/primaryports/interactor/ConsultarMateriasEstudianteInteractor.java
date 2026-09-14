package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.mapper.ConsultarMateriasEstudianteMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarMateriasEstudianteInteractor implements ConsultarMateriasEstudianteInputPort {
    private final ConsultarMateriasEstudianteUseCase useCase;
    public ConsultarMateriasEstudianteInteractor(final ConsultarMateriasEstudianteUseCase useCase) { this.useCase = Objects.requireNonNull(useCase); }
    @Override public List<MateriaEstudianteDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarMateriasEstudianteMapper::toDTO).toList();
    }
}
