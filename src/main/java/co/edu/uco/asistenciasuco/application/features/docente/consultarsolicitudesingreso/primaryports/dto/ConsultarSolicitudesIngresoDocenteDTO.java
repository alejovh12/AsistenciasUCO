package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto;

import java.util.UUID;

/** Entrada: grupo obligatorio; estado (codigo o nombre) y paginacion opcionales. */
public record ConsultarSolicitudesIngresoDocenteDTO(UUID actorUsuarioId, UUID grupoId, String estado, Integer pagina, Integer tamano) {
}
