package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

public final class ResolverSolicitudRevisionAsistenciaMapper {

    private ResolverSolicitudRevisionAsistenciaMapper() {
    }

    public static ResolverSolicitudRevisionAsistenciaDomain toDomain(final ResolverSolicitudRevisionAsistenciaDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para resolver solicitud de revision es obligatorio.");
        }
        return new ResolverSolicitudRevisionAsistenciaDomain(
                dto.getSolicitud(),
                dto.getAccion(),
                dto.getRespuestaDocente(),
                dto.getUsuario()
        );
    }
}
