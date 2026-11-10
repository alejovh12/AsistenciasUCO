package co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * DEC-065 (2): el coordinador del programa aprueba la solicitud de sesion extraordinaria del docente. La DB valida el rol y el ambito
 * ({@code SEC_001}/{@code SEC_002}), que exista solicitud ({@code SES_025}) y que siga pendiente ({@code SES_024}).
 */
public interface AprobarSolicitudSesionInputPort
        extends InteractorWithReturn<AprobarSolicitudSesionInputPort.Comando, AprobarSolicitudSesionInputPort.Resultado> {

    record Comando(UUID sesionId, String observacion, UUID usuarioEjecutor) {
    }

    /**
     * @param mensajeUsuario         el del SP ({@code SES_026}), listo para presentar
     * @param estudiantesNotificados estudiantes cubiertos por el aviso agrupado que se encolo (0 si no se encolo)
     * @param docenteNotificado      {@code true} si se encolo el aviso al docente
     * @param eventoPublicado        {@code true} si se publico {@code SESION_MANUAL_CREADA}
     */
    record Resultado(UUID idSesion, UUID idGrupo, String mensajeUsuario, int totalEstudiantesActivos, int estudiantesNotificados,
                     boolean docenteNotificado, boolean eventoPublicado) {
    }
}
