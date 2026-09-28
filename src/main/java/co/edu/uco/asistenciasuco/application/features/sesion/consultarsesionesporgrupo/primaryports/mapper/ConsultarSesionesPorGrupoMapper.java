package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper.ConsultarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.primaryports.dto.ConsultarSesionesPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;

/**
 * Mapper para convertir entre DTOs y modelos internos del caso de uso.
 */
public final class ConsultarSesionesPorGrupoMapper {

    private ConsultarSesionesPorGrupoMapper() {
    }

    public static ConsultarSesionesPorGrupoDomain toDomain(final ConsultarSesionesPorGrupoDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar sesiones por grupo es obligatorio.");
        }

        return new ConsultarSesionesPorGrupoDomain(dto.getGrupo(), dto.getUsuarioEjecutor());
    }

    public static List<SesionConsultadaDTO> toDTOs(final List<SesionConsultadaEntity> entities) {
        if (ObjectHelper.isNull(entities)) {
            return List.of();
        }

        return entities.stream().map(ConsultarSesionMapper::toDTO).toList();
    }
}
