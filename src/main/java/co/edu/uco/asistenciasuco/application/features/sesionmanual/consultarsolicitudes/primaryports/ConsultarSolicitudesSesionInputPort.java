package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DEC-065 (2): bandeja de solicitudes de sesion extraordinaria de los programas del coordinador. El ambito (programas adscritos) y el
 * rol los fuerza el SP.
 */
public interface ConsultarSolicitudesSesionInputPort
        extends InteractorWithReturn<ConsultarSolicitudesSesionInputPort.Comando, ConsultarSolicitudesSesionInputPort.Resultado> {

    /** {@code pagina} y {@code tamano} nulos toman 1 y 20. */
    record Comando(UUID usuarioId, boolean soloPendientes, UUID programaId, UUID grupoId, Integer pagina, Integer tamano) {
    }

    /** Las horas son instantes UTC. */
    record Solicitud(UUID idSesion, String nombreSesion, String codigoSesion, UUID idGrupo, String nombreGrupo, String nombreAsignatura,
                     UUID idPrograma, String nombrePrograma, UUID idDocente, String nombreDocente, Instant fechaHoraInicio,
                     Instant fechaHoraFin, String estadoSolicitud, String nombreEstadoSolicitud, Instant fechaSolicitudUtc,
                     Instant fechaResolucionUtc, String observacion) {
    }

    record Resultado(List<Solicitud> elementos, long total, int pagina, int tamano) {
    }
}
