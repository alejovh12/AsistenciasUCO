package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.domain.ReclamoEstudianteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;

public final class ConsultarReclamosEstudianteRepositoryMapper {

    private ConsultarReclamosEstudianteRepositoryMapper() {
    }

    public static ReclamoEstudianteDomain toDomain(final SolicitudRevisionEstudianteProjection p) {
        return new ReclamoEstudianteDomain(
                p.idSolicitud(), p.nombreSolicitud(), p.fechaSolicitud(), p.codigoEstado(),
                p.justificacionSolicitud(), p.justificacionRespuesta(), p.idSesion(), p.numeroSesion(),
                p.fechaHoraInicioSesionUtc(), p.procedenciaTemporalSesion(), p.idGrupo(), p.codigoGrupo(),
                p.nombreGrupo(), p.idAsignatura(), p.codigoAsignatura(), p.nombreAsignatura(),
                p.idArchivoSoporte(), p.nombreSoporte(), p.tipoContenidoSoporte(), p.tamanioSoporteBytes(),
                p.fechaRegistroSoporteUtc());
    }
}
