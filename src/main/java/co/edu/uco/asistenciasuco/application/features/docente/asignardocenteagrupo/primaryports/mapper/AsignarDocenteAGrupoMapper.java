package co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.primaryports.dto.AsignarDocenteAGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.domain.AsignarDocenteAGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.asignardocenteagrupo.usecase.entity.AsignarDocenteAGrupoResultadoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

/**
 * Mapper para convertir entre DTOs y modelos internos de asignar docente a grupo.
 */
public final class AsignarDocenteAGrupoMapper {

    private AsignarDocenteAGrupoMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static AsignarDocenteAGrupoDomain toDomain(final AsignarDocenteAGrupoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para asignar docente a grupo es obligatorio.");
        }

        return AsignarDocenteAGrupoDomain.crear(
                dto.getDocente(),
                dto.getGrupo()
        );
    }

    public static AsignarDocenteAGrupoResultadoDTO toDTO(final AsignarDocenteAGrupoResultadoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El resultado de asignar docente a grupo es obligatorio.");
        }

        return new AsignarDocenteAGrupoResultadoDTO(
                entity.isExitoso(),
                entity.getMensajeUsuario()
        );
    }
}
