package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dia de receso academico recien creado (DEC-081). {@code sesionesCanceladas} son las clases canceladas al instante en los grupos del
 * periodo; {@code sesionesConAsistencia} las que se respetaron por tener asistencia o toma.
 */
public record RecesoAcademicoCreadoResponse(UUID id, LocalDate fecha, String motivo, int sesionesCanceladas, int sesionesConAsistencia) {

    public static RecesoAcademicoCreadoResponse from(final CrearRecesoAcademicoInputPort.Resultado resultado) {
        return new RecesoAcademicoCreadoResponse(resultado.id(), resultado.fecha(), resultado.motivo(), resultado.sesionesCanceladas(),
                resultado.sesionesConAsistencia());
    }
}