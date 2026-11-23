package co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DEC-081: dias de receso activos de un periodo, ordenados por fecha. Los lee cualquier perfil de la institucion del periodo
 * (el ambito lo aplica la DB: {@code SEC_001} sin perfil, {@code PER_001} periodo inexistente o de otra institucion).
 */
public interface ConsultarRecesosPeriodoInputPort extends InteractorWithReturn<ConsultarRecesosPeriodoInputPort.Comando, List<ConsultarRecesosPeriodoInputPort.RecesoDia>> {

    record Comando(UUID idPeriodo, UUID usuarioEjecutor) {
    }

    /** Dia de receso del periodo; {@code estaAplicado} = ya cancelo clases, {@code puedeRetirarse} = todavia no cancelo ninguna. */
    record RecesoDia(UUID id, LocalDate fecha, String motivo, int sesionesCanceladas, boolean estaAplicado, boolean puedeRetirarse) {
    }
}
