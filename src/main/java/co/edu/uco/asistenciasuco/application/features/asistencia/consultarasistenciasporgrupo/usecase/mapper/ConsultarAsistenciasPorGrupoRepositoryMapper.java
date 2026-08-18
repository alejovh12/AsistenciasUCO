package co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.domain.ConsultarAsistenciasPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.entity.AsistenciaConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.AsistenciaRepositoryEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper entre el caso de uso y el contrato del puerto secundario.
 */
public final class ConsultarAsistenciasPorGrupoRepositoryMapper {

    private ConsultarAsistenciasPorGrupoRepositoryMapper() {
        throw new CrosscuttingException("No es permitido instanciar una clase utilitaria.");
    }

    public static ConsultarAsistenciasPorGrupoRepositoryDTO toRepositoryDTO(
            final ConsultarAsistenciasPorGrupoDomain domain
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        return new ConsultarAsistenciasPorGrupoRepositoryDTO(
                domain.getGrupo(),
                domain.getSesion()
        );
    }

    public static List<AsistenciaConsultadaEntity> toUseCaseEntities(final List<AsistenciaRepositoryEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        final List<AsistenciaConsultadaEntity> resultado = new ArrayList<>();

        for (final AsistenciaRepositoryEntity entity : entities) {
            resultado.add(new AsistenciaConsultadaEntity(
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
