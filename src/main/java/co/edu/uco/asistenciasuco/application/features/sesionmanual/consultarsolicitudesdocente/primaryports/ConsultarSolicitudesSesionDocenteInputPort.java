package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * ATA D6: lista de solo lectura de las solicitudes de sesion extraordinaria del docente titular (sus grupos). El rol DOCENTE y la
 * titularidad los fuerza el SP; el resultado es el mismo de la bandeja del coordinador.
 */
public interface ConsultarSolicitudesSesionDocenteInputPort
        extends InteractorWithReturn<ConsultarSolicitudesSesionDocenteInputPort.Comando, Resultado> {

    /** {@code pagina} y {@code tamano} nulos toman 1 y 20; {@code soloPendientes} falso incluye aprobadas, rechazadas y canceladas. */
    record Comando(UUID usuarioId, boolean soloPendientes, UUID grupoId, Integer pagina, Integer tamano) {
    }
}
