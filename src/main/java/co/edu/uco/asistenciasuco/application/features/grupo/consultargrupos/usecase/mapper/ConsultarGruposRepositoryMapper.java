package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.GrupoRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre la consulta de grupos y el puerto secundario.
 */
public final class ConsultarGruposRepositoryMapper {

    private ConsultarGruposRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static List<GrupoEntity> toUseCaseEntities(final List<GrupoRepositoryEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<GrupoEntity> resultado = new ArrayList<>();
        for (final GrupoRepositoryEntity entity : entities) {
            resultado.add(toUseCaseEntity(entity));
        }
        return resultado;
    }

    private static GrupoEntity toUseCaseEntity(final GrupoRepositoryEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El grupo consultado es obligatorio.");
        }

        return new GrupoEntity(
                entity.getId(),
                entity.getCodigo(),
                entity.getNombre(),
                entity.getIdAsignatura(),
                entity.getNombreAsignatura(),
                entity.getIdDocente(),
                entity.getCapacidadMaximaPermitida(),
                entity.getEstudiantesActivos(),
                entity.getCuposDisponibles(),
                entity.isGrupoHabilitado(),
                entity.getFechaInicioPeriodoAcademico(),
                entity.getFechaFinPeriodoAcademico()
        );
    }
}
