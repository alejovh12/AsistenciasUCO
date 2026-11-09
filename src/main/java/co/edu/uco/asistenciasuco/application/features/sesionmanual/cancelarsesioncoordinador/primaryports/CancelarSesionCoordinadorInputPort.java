package co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): el coordinador del programa cancela una sesion creada manualmente por el docente, con un motivo del
 * catalogo y una observacion opcional. La DB valida el ambito ({@code SEC_002}), que la sesion sea manual ({@code SES_018}) y el
 * resto de reglas de cancelacion.
 */
public interface CancelarSesionCoordinadorInputPort
        extends InteractorWithReturn<CancelarSesionCoordinadorInputPort.Comando, CancelarSesionCoordinadorInputPort.Resultado> {

    record Comando(UUID sesionId, String codigoMotivo, String observacion, UUID usuarioEjecutor) {
    }

    record Resultado(UUID idSesion, UUID idGrupo, EstadoSesion estado, CancelacionSesionDTO cancelacion, int totalEstudiantesActivos) {
    }
}
