package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto;

import java.util.UUID;

/** Consulta de estudiantes de los programas del coordinador (CULM-001 B2NM, O2-B2): programaId opcional (S4), filtros y paginacion. */
public record ConsultarEstudiantesProgramaDTO(UUID actorUsuarioId, UUID programaId, String busqueda, Boolean estaActivo, Integer pagina, Integer tamano) {
}
