package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.AsignarDocenteAGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.mapper.AsignarDocenteAGrupoMapper;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.AsignarDocenteAGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Interactor del puerto de entrada para asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoInteractor implements AsignarDocenteAGrupoInputPort {

    private final AsignarDocenteAGrupoUseCase useCase;

    public AsignarDocenteAGrupoInteractor(final AsignarDocenteAGrupoUseCase useCase) {
        if (ObjectHelper.isNull(useCase)) {
            throw new CrosscuttingException("El caso de uso AsignarDocenteAGrupoUseCase es obligatorio.");
        }
        this.useCase = useCase;
    }

    @Override
    public AsignarDocenteAGrupoResultadoDTO execute(final AsignarDocenteAGrupoDTO dto) {
        final AsignarDocenteAGrupoDomain domain = AsignarDocenteAGrupoMapper.toDomain(dto);
        return AsignarDocenteAGrupoMapper.toDTO(useCase.execute(domain));
    }
}
