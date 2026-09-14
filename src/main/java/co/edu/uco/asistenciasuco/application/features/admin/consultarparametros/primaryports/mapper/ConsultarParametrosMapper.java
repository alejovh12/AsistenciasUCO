package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.dto.ParametroDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.domain.ParametroDomain;

public final class ConsultarParametrosMapper {

    private ConsultarParametrosMapper() {
    }

    public static ParametroDTO toDTO(final ParametroDomain domain) {
        return new ParametroDTO(domain.id(), domain.grupo(), domain.clave(), domain.valor(), domain.tipoDato(),
                domain.valorDefecto(), domain.estaActivo());
    }
}
