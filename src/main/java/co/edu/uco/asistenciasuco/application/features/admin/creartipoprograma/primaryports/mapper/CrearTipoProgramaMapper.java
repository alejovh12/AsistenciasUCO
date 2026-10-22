package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto.CrearTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.domain.CrearTipoProgramaDomain;

public final class CrearTipoProgramaMapper {

    private CrearTipoProgramaMapper() {
    }

    public static CrearTipoProgramaDomain toDomain(final CrearTipoProgramaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CrearTipoProgramaDomain(dto.codigo(), dto.nombre(), dto.actorUsuarioId());
    }
}
