package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;

import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): respuesta de {@code POST /api/v1/coordinador/sesiones/{sesionId}/cancelacion}.
 * {@code cancelacion.tipo} es {@code COORDINADOR} y {@code cancelacion.motivo} el motivo guardado (nombre del catalogo mas la
 * observacion). {@code estudiantesActivos} es informativo: el aviso a estudiantes llega por el evento {@code SESION_CANCELADA}.
 */
public record SesionCanceladaCoordinadorResponse(
        UUID idSesion,
        UUID idGrupo,
        String estado,
        CancelacionSesionResponse cancelacion,
        int estudiantesActivos
) {

    public static SesionCanceladaCoordinadorResponse from(final CancelarSesionCoordinadorInputPort.Resultado resultado) {
        return new SesionCanceladaCoordinadorResponse(resultado.idSesion(), resultado.idGrupo(), resultado.estado().name(),
                CancelacionSesionResponse.from(resultado.cancelacion()), resultado.totalEstudiantesActivos());
    }
}
