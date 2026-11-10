package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Solicitud;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * DEC-065 (2): fila de {@code GET /api/v1/coordinador/sesiones/solicitudes}. Las horas son ISO-8601 UTC con {@code Z}; los textos de
 * estado ({@code nombreEstadoSolicitud}) los entrega la DB.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SolicitudSesionResponse(
        UUID idSesion,
        String nombreSesion,
        String codigoSesion,
        UUID idGrupo,
        String nombreGrupo,
        String nombreAsignatura,
        UUID idPrograma,
        String nombrePrograma,
        UUID idDocente,
        String nombreDocente,
        String fechaHoraInicio,
        String fechaHoraFin,
        String estadoSolicitud,
        String nombreEstadoSolicitud,
        String fechaSolicitudUtc,
        String fechaResolucionUtc,
        String observacion
) {

    public static SolicitudSesionResponse from(final Solicitud s) {
        return new SolicitudSesionResponse(s.idSesion(), s.nombreSesion(), s.codigoSesion(), s.idGrupo(), s.nombreGrupo(),
                s.nombreAsignatura(), s.idPrograma(), s.nombrePrograma(), s.idDocente(), s.nombreDocente(), texto(s.fechaHoraInicio()),
                texto(s.fechaHoraFin()), s.estadoSolicitud(), s.nombreEstadoSolicitud(), texto(s.fechaSolicitudUtc()),
                texto(s.fechaResolucionUtc()), s.observacion());
    }

    private static String texto(final Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
