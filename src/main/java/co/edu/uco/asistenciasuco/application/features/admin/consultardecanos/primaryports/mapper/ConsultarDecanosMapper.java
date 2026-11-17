package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain.DecanoDomain;

public final class ConsultarDecanosMapper {

    private ConsultarDecanosMapper() {
    }

    public static DecanoDTO toDTO(final DecanoDomain domain) {
        return new DecanoDTO(domain.id(), domain.idUsuario(), domain.numeroIdentificacion(), domain.nombreCompleto(),
                domain.idFacultad(), domain.nombreFacultad(), domain.estaActivoDecano(), domain.primerNombre(),
                domain.segundoNombre(), domain.primerApellido(), domain.segundoApellido(),
                TextHelper.unirNombres(domain.primerNombre(), domain.segundoNombre()),
                TextHelper.unirNombres(domain.primerApellido(), domain.segundoApellido()));
    }
}
