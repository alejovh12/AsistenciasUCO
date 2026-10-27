package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Destinatario del aviso de retiro (HU051) con el contexto del grupo para redactar el mensaje. */
public record DestinatarioRetiroProjection(
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
