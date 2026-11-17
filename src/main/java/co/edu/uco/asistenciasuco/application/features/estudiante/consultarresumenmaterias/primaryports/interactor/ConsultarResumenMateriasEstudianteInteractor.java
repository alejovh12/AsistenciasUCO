package co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.ConsultarResumenMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.primaryports.dto.ResumenMateriasEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.ConsultarResumenMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarresumenmaterias.usecase.domain.ResumenMateriasEstudianteDomain;

import java.util.Objects;
import java.util.UUID;

public final class ConsultarResumenMateriasEstudianteInteractor implements ConsultarResumenMateriasEstudianteInputPort {

    private final ConsultarResumenMateriasEstudianteUseCase useCase;

    public ConsultarResumenMateriasEstudianteInteractor(final ConsultarResumenMateriasEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ResumenMateriasEstudianteDTO execute(final UUID actorUsuarioId) {
        final ResumenMateriasEstudianteDomain d = useCase.execute(actorUsuarioId);
        return new ResumenMateriasEstudianteDTO(d.totalMaterias(), d.materiasEnRiesgo(), d.totalHorasFalta(), d.totalCreditos());
    }
}
