package co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CIERRE-W3M (puntos 2 y 3): resumen de las sesiones del grupo del docente titular (estado temporal y conteos por sesion,
 * conteos del grupo, sesion en curso o proxima, estudiantes en riesgo y promedio). Lo calcula la base; el cliente no usa su reloj.
 */
public interface ConsultarResumenSesionesGrupoInputPort
        extends InteractorWithReturn<ConsultarResumenSesionesGrupoInputPort.Consulta, ResumenSesionesGrupoDTO> {

    /** Entrada: grupo y ejecutor (usuario autenticado). */
    record Consulta(UUID grupoId, UUID usuarioEjecutor) {
    }
}
