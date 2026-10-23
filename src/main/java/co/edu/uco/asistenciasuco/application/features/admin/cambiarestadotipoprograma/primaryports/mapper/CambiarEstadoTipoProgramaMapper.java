package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.domain.CambiarEstadoTipoProgramaDomain;

public final class CambiarEstadoTipoProgramaMapper {

    private CambiarEstadoTipoProgramaMapper() {
    }

    public static CambiarEstadoTipoProgramaDomain toDomain(final CambiarEstadoTipoProgramaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CambiarEstadoTipoProgramaDomain(dto.id(), dto.activo(), dto.actorUsuarioId());
    }
}
