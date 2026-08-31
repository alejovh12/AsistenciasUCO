package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.ConsultarAsignacionesAcademicasDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto.DocenteAsignacionAcademicaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir entre asignaciones academicas internas y DTOs.
 */
public final class ConsultarAsignacionesAcademicasDocenteMapper {

    private ConsultarAsignacionesAcademicasDocenteMapper() {
    }

    public static ConsultarAsignacionesAcademicasDocenteDomain toDomain(
            final ConsultarAsignacionesAcademicasDocenteDTO dto
    ) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar asignaciones academicas del docente es obligatorio.");
        }

        return ConsultarAsignacionesAcademicasDocenteDomain.crear(dto.getDocente());
    }

    public static List<DocenteAsignacionAcademicaDTO> toDTOs(
            final List<DocenteAsignacionAcademicaEntity> entities
    ) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<DocenteAsignacionAcademicaDTO> resultado = new ArrayList<>();
        for (final DocenteAsignacionAcademicaEntity entity : entities) {
            resultado.add(toDTO(entity));
        }
        return resultado;
    }

    private static DocenteAsignacionAcademicaDTO toDTO(final DocenteAsignacionAcademicaEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La asignacion academica del docente es obligatoria.");
        }

        return new DocenteAsignacionAcademicaDTO(
                entity.getId(),
                entity.getIdUsuario(),
                entity.getNumeroIdentificacion(),
                entity.getNombreCompleto(),
                entity.isEstaActivoUsuario(),
                entity.getIdInstitucion(),
                entity.getNombreInstitucion(),
                entity.getIdFacultad(),
                entity.getNombreFacultad(),
                entity.getIdPrograma(),
                entity.getNombrePrograma(),
                entity.getIdPlanEstudio(),
                entity.getInpPlanEstudio(),
                entity.getIdAsignatura(),
                entity.getNombreAsignatura(),
                entity.getIdGrupo(),
                entity.getNombreGrupo(),
                entity.getIdPerfil(),
                entity.getCodigoPerfil(),
                entity.getNombrePerfil(),
                entity.isEstaActivoDocente(),
                entity.getEstaActivoTextoDocente()
        );
    }
}
