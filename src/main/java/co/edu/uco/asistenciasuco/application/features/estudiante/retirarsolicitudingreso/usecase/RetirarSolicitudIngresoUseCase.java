package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase;

import java.util.UUID;

public interface RetirarSolicitudIngresoUseCase {

    void execute(UUID actorUsuarioId, UUID solicitudId);
}
