package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto.DocenteIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir identidades docentes internas a DTOs.
 */
public final class ConsultarDocentesMapper {

    private ConsultarDocentesMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static List<DocenteIdentidadDTO> toDTOs(final List<DocenteIdentidadEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<DocenteIdentidadDTO> resultado = new ArrayList<>();
        for (final DocenteIdentidadEntity entity : entities) {
            resultado.add(toDTO(entity));
        }
        return resultado;
    }

    public static DocenteIdentidadDTO toDTO(final DocenteIdentidadEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La identidad del docente es obligatoria.");
        }

        return new DocenteIdentidadDTO(
                entity.getId(),
                entity.getIdUsuario(),
                entity.getNumeroIdentificacion(),
                entity.getNombreCompleto(),
                entity.isEstaActivoUsuario()
        );
    }
}
