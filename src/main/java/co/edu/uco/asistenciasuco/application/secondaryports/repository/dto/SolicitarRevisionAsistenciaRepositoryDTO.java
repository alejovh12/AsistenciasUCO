package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.List;
import java.util.UUID;

/**
 * Radicacion de un reclamo. CULM-001 D.2: {@code soportes} son los fileId (DRAFT) de 0 a N soportes, en el orden recibido.
 */
public record SolicitarRevisionAsistenciaRepositoryDTO(
        UUID estudiante,
        UUID sesion,
        String categoria,
        String justificacion,
        List<UUID> soportes,
        UUID usuarioEjecutor
) {
}
