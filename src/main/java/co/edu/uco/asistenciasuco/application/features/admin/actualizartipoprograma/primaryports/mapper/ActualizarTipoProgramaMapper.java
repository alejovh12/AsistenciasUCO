package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.domain.ActualizarTipoProgramaDomain;

public final class ActualizarTipoProgramaMapper {

    private ActualizarTipoProgramaMapper() {
    }

    public static ActualizarTipoProgramaDomain toDomain(final ActualizarTipoProgramaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new ActualizarTipoProgramaDomain(dto.id(), dto.nombre(), dto.actorUsuarioId());
    }
}
