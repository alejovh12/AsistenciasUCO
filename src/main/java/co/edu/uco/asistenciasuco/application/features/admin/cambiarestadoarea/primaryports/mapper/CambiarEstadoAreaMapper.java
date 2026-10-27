package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.domain.CambiarEstadoAreaDomain;

public final class CambiarEstadoAreaMapper {

    private CambiarEstadoAreaMapper() {
    }

    public static CambiarEstadoAreaDomain toDomain(final CambiarEstadoAreaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CambiarEstadoAreaDomain(dto.id(), dto.activa(), dto.actorUsuarioId());
    }
}
