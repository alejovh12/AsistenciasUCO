package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain;

import java.util.UUID;

/** Consulta de estudiantes de los programas del coordinador. */
public record ConsultarEstudiantesProgramaDomain(UUID actorUsuarioId, UUID programaId, String busqueda, Boolean estaActivo, Integer pagina, Integer tamano) {
}
