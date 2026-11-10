package co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * DEC-065 (2): el coordinador del programa rechaza la solicitud de sesion extraordinaria del docente con un motivo del catalogo
 * ({@code SES_019} si no existe) y una observacion opcional ({@code SES_021}). La sesion queda cancelada.
 */
public interface RechazarSolicitudSesionInputPort
        extends InteractorWithReturn<RechazarSolicitudSesionInputPort.Comando, RechazarSolicitudSesionInputPort.Resultado> {

    record Comando(UUID sesionId, String codigoMotivo, String observacion, UUID usuarioEjecutor) {
    }

    /**
     * @param mensajeUsuario    el del SP ({@code SES_027}), listo para presentar
     * @param docenteNotificado {@code true} si se encolo el aviso al docente
     * @param eventoPublicado   {@code true} si se publico {@code SESION_CANCELADA}
     */
    record Resultado(UUID idSesion, UUID idGrupo, String mensajeUsuario, int totalEstudiantesActivos, boolean docenteNotificado,
                     boolean eventoPublicado) {
    }
}
