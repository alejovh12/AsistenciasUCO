package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarresumensesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.ResumenTomasSesionFila;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;
import java.util.UUID;

/**
 * PC (puntos de control): resumen por estudiante de una sesion del docente titular, en minutos y con el estado de la sesion
 * para cada estudiante; lo calcula la base de datos a partir de los tramos entre puntos de control.
 */
public interface ConsultarResumenTomasSesionInputPort
        extends InteractorWithReturn<ConsultarResumenTomasSesionInputPort.Consulta, List<ResumenTomasSesionFila>> {

    record Consulta(UUID sesionId, UUID usuarioEjecutor) {
    }
}
