package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * Resolucion de un reclamo. PC (puntos de control): {@code codigoResultadoAprobacion} es AN (estuvo presente) o EX (ausencia
 * justificada) y solo aplica con {@code accion = APROBADA}; nulo deja que la base de datos aplique EX.
 */
public record ResolverSolicitudRevisionAsistenciaRepositoryDTO(
        UUID solicitud,
        UUID docente,
        String accion,
        String respuestaDocente,
        UUID usuarioEjecutor,
        String codigoResultadoAprobacion
) {

    public ResolverSolicitudRevisionAsistenciaRepositoryDTO(final UUID solicitud, final UUID docente, final String accion,
                                                            final String respuestaDocente, final UUID usuarioEjecutor) {
        this(solicitud, docente, accion, respuestaDocente, usuarioEjecutor, null);
    }
}
