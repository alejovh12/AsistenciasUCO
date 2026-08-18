package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.domain.ConsultarAsignacionesAcademicasDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.usecase.entity.DocenteAsignacionAcademicaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsignacionesAcademicasDocenteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.DocenteAsignacionAcademicaRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre la consulta de asignaciones academicas y el puerto secundario.
 */
public final class ConsultarAsignacionesAcademicasDocenteRepositoryMapper {

    private ConsultarAsignacionesAcademicasDocenteRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static ConsultarAsignacionesAcademicasDocenteRepositoryDTO toRepositoryDTO(
            final ConsultarAsignacionesAcademicasDocenteDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException(
                    "El dominio para consultar asignaciones academicas del docente es obligatorio."
            );
        }

        return new ConsultarAsignacionesAcademicasDocenteRepositoryDTO(domain.getDocente());
    }

    public static List<DocenteAsignacionAcademicaEntity> toUseCaseEntities(
            final List<DocenteAsignacionAcademicaRepositoryEntity> entities
    ) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<DocenteAsignacionAcademicaEntity> resultado = new ArrayList<>();
        for (final DocenteAsignacionAcademicaRepositoryEntity entity : entities) {
            resultado.add(toUseCaseEntity(entity));
        }
        return resultado;
    }

    private static DocenteAsignacionAcademicaEntity toUseCaseEntity(
            final DocenteAsignacionAcademicaRepositoryEntity entity
    ) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La asignacion academica del docente consultado es obligatoria.");
        }

        return new DocenteAsignacionAcademicaEntity(
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
                toBooleanEstricto(entity.getEstaActivoDocente()),
                entity.getEstaActivoTextoDocente()
        );
    }

    private static boolean toBooleanEstricto(final Integer value) {
        if (ObjectHelper.isNull(value)) {
            throw new CrosscuttingException("El estado activo del docente es obligatorio en dbo.uv_docente.");
        }
        if (value == 0) {
            return false;
        }
        if (value == 1) {
            return true;
        }
        throw new CrosscuttingException("El estado activo del docente incumple el contrato interno.");
    }
}
