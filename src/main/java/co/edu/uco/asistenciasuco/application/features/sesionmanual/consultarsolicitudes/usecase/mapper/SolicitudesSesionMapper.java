package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Solicitud;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * DEC-065 (2) y ATA D6: traduce la pagina de solicitudes de la DB (DATETIME2 en UTC sin zona) al resultado de aplicacion con instantes.
 * Lo comparten la bandeja del coordinador y la lista de solo lectura del docente.
 */
public final class SolicitudesSesionMapper {

    private SolicitudesSesionMapper() {
    }

    public static Resultado aResultado(final PaginaSolicitudesSesion datos, final int pagina, final int tamano) {
        return new Resultado(datos.elementos().stream().map(SolicitudesSesionMapper::aSolicitud).toList(), datos.total(), pagina, tamano);
    }

    private static Solicitud aSolicitud(final SolicitudSesion s) {
        return new Solicitud(s.idSesion(), s.nombreSesion(), s.codigoSesion(), s.idGrupo(), s.nombreGrupo(), s.nombreAsignatura(),
                s.idPrograma(), s.nombrePrograma(), s.idDocente(), s.nombreDocente(), instante(s.fechaHoraInicio()),
                instante(s.fechaHoraFin()), s.estadoSolicitud(), s.nombreEstadoSolicitud(), instante(s.fechaSolicitudUtc()),
                instante(s.fechaResolucionUtc()), s.observacion(), s.nombreProgramaResponsable(),
                s.nombreCoordinadorResolutor());
    }

    private static Instant instante(final LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
