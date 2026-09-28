package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.UUID;

public final class ResolverSolicitudRevisionAsistenciaRepositoryMapper {

    private ResolverSolicitudRevisionAsistenciaRepositoryMapper() {
    }

    public static ResolverSolicitudRevisionAsistenciaRepositoryDTO toRepositoryDTO(
            final ResolverSolicitudRevisionAsistenciaDomain domain,
            final UUID docenteId
    ) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para resolver solicitud de revision es obligatorio.");
        }
        return new ResolverSolicitudRevisionAsistenciaRepositoryDTO(
                domain.getSolicitud(),
                docenteId,
                domain.getAccion(),
                domain.getRespuestaDocente(),
                domain.getUsuario()
        );
    }
}
