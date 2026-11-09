package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;
import java.util.UUID;

/** CULM-001 / DEC-064: motivos con los que el coordinador puede cancelar una sesion manual (catalogo de la DB). */
public interface ConsultarMotivosCancelacionInputPort
        extends InteractorWithReturn<ConsultarMotivosCancelacionInputPort.Comando, List<ConsultarMotivosCancelacionInputPort.Motivo>> {

    record Comando(UUID usuarioEjecutor) {
    }

    record Motivo(String codigo, String nombre) {
    }
}
