package co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.entity.EstudianteGrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;

import java.util.List;

public final class ConsultarEstudiantesGrupoRepositoryMapper {

    private ConsultarEstudiantesGrupoRepositoryMapper() {
    }

    public static List<EstudianteGrupoEntity> toEntities(final List<EstudianteGrupoRepositoryProjection> projections) {
        return projections.stream().map(ConsultarEstudiantesGrupoRepositoryMapper::toEntity).toList();
    }

    private static EstudianteGrupoEntity toEntity(final EstudianteGrupoRepositoryProjection projection) {
        return new EstudianteGrupoEntity(
                projection.id(),
                projection.idEstudiante(),
                projection.documento(),
                projection.nombreCompleto(),
                projection.correo(),
                projection.codigoEstado(),
                projection.nombreEstado()
        );
    }
}
