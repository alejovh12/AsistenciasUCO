package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.List;
import java.util.UUID;

/**
 * Radicacion de un reclamo. CULM-001 D.2: {@code soportes} son los fileId (DRAFT) de 0 a N soportes, en el orden recibido.
 * PC (puntos de control): {@code toma} nula radica el reclamo de la sesion (v3, legado); con toma, el reclamo de esa toma (v4).
 */
public record SolicitarRevisionAsistenciaRepositoryDTO(
        UUID estudiante,
        UUID sesion,
        UUID toma,
        String categoria,
        String justificacion,
        List<UUID> soportes,
        UUID usuarioEjecutor
) {

    /** Reclamo de la sesion completa (legado, sin toma). */
    public SolicitarRevisionAsistenciaRepositoryDTO(final UUID estudiante, final UUID sesion, final String categoria,
                                                    final String justificacion, final List<UUID> soportes,
                                                    final UUID usuarioEjecutor) {
        this(estudiante, sesion, null, categoria, justificacion, soportes, usuarioEjecutor);
    }
}
