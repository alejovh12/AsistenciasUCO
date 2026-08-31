package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir grupos internos a DTOs.
 */
public final class ConsultarGruposMapper {

    private ConsultarGruposMapper() {
    }

    public static List<GrupoDTO> toDTOs(final List<GrupoEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<GrupoDTO> resultado = new ArrayList<>();
        for (final GrupoEntity entity : entities) {
            resultado.add(toDTO(entity));
        }
        return resultado;
    }

    private static GrupoDTO toDTO(final GrupoEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("El grupo consultado es obligatorio.");
        }

        return new GrupoDTO(
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
