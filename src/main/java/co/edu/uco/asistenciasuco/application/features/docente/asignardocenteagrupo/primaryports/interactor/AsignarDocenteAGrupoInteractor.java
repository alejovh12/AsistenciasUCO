package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.mapper.AsignarDocenteAGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.AsignarDocenteAGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import java.util.Objects;

/**
 * Interactor del puerto de entrada para asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoInteractor implements AsignarDocenteAGrupoInputPort {

    private final AsignarDocenteAGrupoUseCase useCase;

    public AsignarDocenteAGrupoInteractor(final AsignarDocenteAGrupoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso AsignarDocenteAGrupoUseCase es obligatorio.");
    }

    @Override
    public AsignarDocenteAGrupoResultadoDTO execute(final AsignarDocenteAGrupoDTO dto) {
        final AsignarDocenteAGrupoDomain domain = AsignarDocenteAGrupoMapper.toDomain(dto);
        return AsignarDocenteAGrupoMapper.toDTO(useCase.execute(domain));
    }
}