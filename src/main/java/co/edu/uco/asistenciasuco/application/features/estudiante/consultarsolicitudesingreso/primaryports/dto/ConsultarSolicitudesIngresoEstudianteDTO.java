package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto;

import java.util.UUID;

/** Entrada: el estudiante se identifica solo por el usuario autenticado; estado y paginacion opcionales. */
public record ConsultarSolicitudesIngresoEstudianteDTO(UUID actorUsuarioId, String estado, Integer pagina, Integer tamano) {
}
