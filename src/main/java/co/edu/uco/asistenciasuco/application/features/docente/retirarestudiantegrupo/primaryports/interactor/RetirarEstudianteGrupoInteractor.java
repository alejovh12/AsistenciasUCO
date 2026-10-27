package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto.RetirarEstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.RetirarEstudianteGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain.RetirarEstudianteGrupoDomain;

import java.util.Objects;

public final class RetirarEstudianteGrupoInteractor implements RetirarEstudianteGrupoInputPort {

    private final RetirarEstudianteGrupoUseCase useCase;

    public RetirarEstudianteGrupoInteractor(final RetirarEstudianteGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final RetirarEstudianteGrupoDTO dto) {
        useCase.execute(RetirarEstudianteGrupoDomain.of(
                dto == null ? null : dto.actorUsuarioId(),
                dto == null ? null : dto.grupoId(),
                dto == null ? null : dto.estudianteId(),
                dto == null ? null : dto.motivo()));
    }
}
