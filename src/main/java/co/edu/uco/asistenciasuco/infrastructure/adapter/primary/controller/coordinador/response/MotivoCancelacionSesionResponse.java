package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort;

/** CULM-001 / DEC-064: motivo del catalogo con el que el coordinador cancela una sesion manual. */
public record MotivoCancelacionSesionResponse(String codigo, String nombre) {

    public static MotivoCancelacionSesionResponse from(final ConsultarMotivosCancelacionInputPort.Motivo motivo) {
        return new MotivoCancelacionSesionResponse(motivo.codigo(), motivo.nombre());
    }
}
