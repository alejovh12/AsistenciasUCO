package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto;

import java.util.UUID;

/** Entrada de HU053: el estudiante se identifica solo por el usuario autenticado. */
public record SolicitarIngresoGrupoDTO(UUID actorUsuarioId, UUID grupoId) {
}
