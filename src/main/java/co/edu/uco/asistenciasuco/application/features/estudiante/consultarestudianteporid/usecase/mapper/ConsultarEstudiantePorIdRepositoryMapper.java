package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteContextoAcademicoEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteDetalleEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.mapper.ConsultarEstudiantesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteContextoAcademicoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.List;

public final class ConsultarEstudiantePorIdRepositoryMapper {

    private ConsultarEstudiantePorIdRepositoryMapper() {
    }

    public static EstudianteDetalleEntity toUseCaseEntity(final EstudianteDetalleRepositoryProjection entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El detalle de estudiante consultado es obligatorio.");
        }
        return new EstudianteDetalleEntity(
                ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(entity.datosPersonales()),
                toUseCaseEntities(entity.contextosAcademicos())
        );
    }

    private static List<EstudianteContextoAcademicoEntity> toUseCaseEntities(
            final List<EstudianteContextoAcademicoRepositoryProjection> entities
    ) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }
        return entities.stream().map(ConsultarEstudiantePorIdRepositoryMapper::toUseCaseEntity).toList();
    }

    private static EstudianteContextoAcademicoEntity toUseCaseEntity(
            final EstudianteContextoAcademicoRepositoryProjection entity
    ) {
        return new EstudianteContextoAcademicoEntity(
                entity.idInstitucion(),
                entity.nombreInstitucion(),
                entity.idFacultad(),
                entity.nombreFacultad(),
                entity.idPrograma(),
                entity.nombrePrograma(),
                entity.idPlanEstudio(),
                entity.inpPlanEstudio(),
                entity.idAsignatura(),
                entity.nombreAsignatura(),
                entity.idGrupo(),
                entity.nombreGrupo()
        );
    }
}
