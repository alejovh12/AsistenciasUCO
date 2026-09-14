package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto.InstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.domain.InstitucionDomain;

public final class ConsultarInstitucionesMapper {

    private ConsultarInstitucionesMapper() {
    }

    public static InstitucionDTO toDTO(final InstitucionDomain domain) {
        return new InstitucionDTO(domain.id(), domain.nombre(), domain.estaActivaInstitucion(), domain.estaActivaTextoInstitucion());
    }
}
