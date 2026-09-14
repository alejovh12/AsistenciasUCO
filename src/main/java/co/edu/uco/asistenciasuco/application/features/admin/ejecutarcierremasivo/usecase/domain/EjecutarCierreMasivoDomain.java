package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion ejecutar cierre masivo de periodo.
 */
public final class EjecutarCierreMasivoDomain {

    private final UUID idPeriodoAcademico;
    private final UUID actorUsuarioId;

    public EjecutarCierreMasivoDomain(final UUID idPeriodoAcademico, final UUID actorUsuarioId) {
        this.idPeriodoAcademico = idPeriodoAcademico;
        this.actorUsuarioId = actorUsuarioId;
    }

    public UUID getIdPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public UUID getActorUsuarioId() {
        return actorUsuarioId;
    }
}
