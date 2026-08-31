package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.DocenteIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre la consulta de docentes y el contrato del puerto secundario.
 */
public final class ConsultarDocentesRepositoryMapper {

    private ConsultarDocentesRepositoryMapper() {
    }

    public static List<DocenteIdentidadEntity> toUseCaseEntities(
            final List<DocenteIdentidadRepositoryProjection> entities
    ) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<DocenteIdentidadEntity> resultado = new ArrayList<>();
        for (final DocenteIdentidadRepositoryProjection entity : entities) {
            resultado.add(toUseCaseEntity(entity));
        }
        return resultado;
    }

    public static DocenteIdentidadEntity toUseCaseEntity(final DocenteIdentidadRepositoryProjection entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La identidad del docente consultado es obligatoria.");
        }

        return new DocenteIdentidadEntity(
                entity.getId(),
                entity.getIdUsuario(),
                entity.getNumeroIdentificacion(),
                entity.getNombreCompleto(),
                entity.isEstaActivoUsuario()
        );
    }
}
