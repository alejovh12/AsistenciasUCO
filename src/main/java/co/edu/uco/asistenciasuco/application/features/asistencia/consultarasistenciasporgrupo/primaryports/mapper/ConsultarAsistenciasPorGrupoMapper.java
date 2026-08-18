package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.AsistenciaConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper para convertir entre DTOs y modelos internos del caso de uso.
 */
public final class ConsultarAsistenciasPorGrupoMapper {

    private ConsultarAsistenciasPorGrupoMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static ConsultarAsistenciasPorGrupoDomain toDomain(final ConsultarAsistenciasPorGrupoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar asistencias por grupo es obligatorio.");
        }

        return new ConsultarAsistenciasPorGrupoDomain(
                dto.getGrupo(),
                dto.getSesion()
        );
    }

    public static List<AsistenciaConsultadaDTO> toDTOs(final List<AsistenciaConsultadaEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<AsistenciaConsultadaDTO> resultado = new ArrayList<>();

        for (final AsistenciaConsultadaEntity entity : entities) {
            resultado.add(new AsistenciaConsultadaDTO(
                    entity.getAsistencia(),
                    entity.getEstudiante(),
                    entity.getGrupo(),
                    entity.getSesion(),
                    entity.isPresente(),
                    entity.getObservacion()
            ));
        }

        return resultado;
    }
}
