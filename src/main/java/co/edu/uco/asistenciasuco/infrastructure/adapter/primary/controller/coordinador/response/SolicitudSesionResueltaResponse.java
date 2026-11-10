package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort;

import java.util.UUID;

/**
 * DEC-065 (2): datos de {@code POST .../aprobacion} y {@code POST .../rechazo}. {@code estado} es el de la solicitud tras la decision
 * ({@code APROBADA} o {@code RECHAZADA}); {@code estudiantesActivos} es informativo.
 */
public record SolicitudSesionResueltaResponse(UUID idSesion, UUID idGrupo, String estado, int estudiantesActivos) {

    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    public static SolicitudSesionResueltaResponse from(final AprobarSolicitudSesionInputPort.Resultado resultado) {
        return new SolicitudSesionResueltaResponse(resultado.idSesion(), resultado.idGrupo(), APROBADA, resultado.totalEstudiantesActivos());
    }

    public static SolicitudSesionResueltaResponse from(final RechazarSolicitudSesionInputPort.Resultado resultado) {
        return new SolicitudSesionResueltaResponse(resultado.idSesion(), resultado.idGrupo(), RECHAZADA, resultado.totalEstudiantesActivos());
    }
}
