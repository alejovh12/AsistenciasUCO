package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto;

import java.util.UUID;

/** Entrada: el estudiante se identifica solo por el usuario autenticado; estado y paginacion son opcionales. */
public record ConsultarReclamosEstudianteDTO(UUID actorUsuarioId, String estado, Integer pagina, Integer tamano) {
}
