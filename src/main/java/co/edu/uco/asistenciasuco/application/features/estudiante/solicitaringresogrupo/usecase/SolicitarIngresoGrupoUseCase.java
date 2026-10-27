package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase;

import java.util.UUID;

public interface SolicitarIngresoGrupoUseCase {

    UUID execute(UUID actorUsuarioId, UUID grupoId);
}
