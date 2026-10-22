package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoDocenteProgramaDTO(UUID actorUsuarioId, UUID idDocente, boolean estaActivo) {
}
