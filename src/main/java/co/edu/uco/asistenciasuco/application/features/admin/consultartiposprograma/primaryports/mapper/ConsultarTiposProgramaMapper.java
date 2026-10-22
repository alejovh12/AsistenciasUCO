package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain.TipoProgramaDomain;

public final class ConsultarTiposProgramaMapper {

    private ConsultarTiposProgramaMapper() {
    }

    public static TipoProgramaDTO toDTO(final TipoProgramaDomain domain) {
        return new TipoProgramaDTO(domain.id(), domain.codigo(), domain.nombre(), domain.activo());
    }
}
