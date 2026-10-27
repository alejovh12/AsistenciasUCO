package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Destinatario de los avisos de una solicitud de ingreso (ESTUDIANTE o DOCENTE) con el contexto del grupo. */
public record DestinatarioSolicitudIngresoProjection(
        String rol,
        UUID idUsuario,
        String nombreCompleto,
        String correo,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        String nombreAsignatura
) {
}
