package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort.RecesoDia;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dia de receso academico de un periodo (DEC-081). {@code estaAplicado} = ya cancelo clases; {@code puedeRetirarse} = todavia no cancelo
 * ninguna (solo entonces el ADMINISTRADOR puede retirarlo).
 */
public record RecesoAcademicoPeriodoResponse(UUID id, LocalDate fecha, String motivo, int sesionesCanceladas, boolean estaAplicado,
                                             boolean puedeRetirarse) {

    public static RecesoAcademicoPeriodoResponse from(final RecesoDia receso) {
        return new RecesoAcademicoPeriodoResponse(receso.id(), receso.fecha(), receso.motivo(), receso.sesionesCanceladas(),
                receso.estaAplicado(), receso.puedeRetirarse());
    }
}