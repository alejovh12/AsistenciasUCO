package co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.domain;

import java.util.UUID;

public record CambiarEstadoDocenteProgramaDomain(UUID actorUsuarioId, UUID idDocente, boolean estaActivo) {
}
