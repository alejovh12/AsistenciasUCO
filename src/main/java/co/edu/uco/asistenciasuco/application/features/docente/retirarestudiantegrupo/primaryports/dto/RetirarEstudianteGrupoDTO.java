package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto;

import java.util.UUID;

/** Entrada de HU051: el docente se identifica solo por el usuario autenticado; el motivo es obligatorio. */
public record RetirarEstudianteGrupoDTO(UUID actorUsuarioId, UUID grupoId, UUID estudianteId, String motivo) {
}
