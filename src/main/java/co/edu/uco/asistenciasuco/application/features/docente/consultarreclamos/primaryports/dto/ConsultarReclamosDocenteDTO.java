package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto;

import java.util.UUID;

/**
 * Entrada: el docente se identifica solo por el usuario autenticado; estado, paginacion, {@code busqueda} (texto libre) y
 * {@code grupoId} (solo ese grupo) son opcionales (CIERRE-W1A).
 */
public record ConsultarReclamosDocenteDTO(
        UUID actorUsuarioId, String estado, Integer pagina, Integer tamano, String busqueda, UUID grupoId) {

    /** Sin busqueda ni grupo (consumidores anteriores a CIERRE-W1A). */
    public ConsultarReclamosDocenteDTO(final UUID actorUsuarioId, final String estado, final Integer pagina, final Integer tamano) {
        this(actorUsuarioId, estado, pagina, tamano, null, null);
    }
}
