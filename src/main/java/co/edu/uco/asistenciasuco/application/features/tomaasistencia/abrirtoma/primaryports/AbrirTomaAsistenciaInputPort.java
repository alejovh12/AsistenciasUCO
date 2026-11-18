package co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013, contrato 3.2.5): el docente titular abre una toma de asistencia ({@code MANUAL} o {@code QR})
 * en una sesion dentro de su ventana; la base de datos asigna el numero, cierra la toma abierta previa (D9) y rechaza el maximo.
 */
public interface AbrirTomaAsistenciaInputPort
        extends InteractorWithReturn<AbrirTomaAsistenciaInputPort.Comando, AbrirTomaAsistenciaInputPort.Resultado> {

    record Comando(UUID sesionId, String modo, UUID usuarioEjecutor) {
    }

    /** D9: la toma que la apertura cerro sola (la que estaba abierta); nula si no habia ninguna. */
    record TomaCerradaAnterior(UUID idToma, int numero, int totalAusentesMarcados) {
    }

    record Resultado(TomaAsistenciaResumen toma, TomaCerradaAnterior tomaCerradaAnterior) {
    }
}
