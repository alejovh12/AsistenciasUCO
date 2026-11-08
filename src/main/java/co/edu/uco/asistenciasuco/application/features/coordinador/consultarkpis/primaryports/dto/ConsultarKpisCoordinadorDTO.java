package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto;

import java.util.UUID;

public record ConsultarKpisCoordinadorDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        UUID idPrograma   // CULM-001 B2NM (S4): opcional; obligatorio de hecho si el coordinador tiene varios programas (COO_002)
) {
    public ConsultarKpisCoordinadorDTO(final UUID actorUsuarioId, final UUID idPeriodoAcademico) {
        this(actorUsuarioId, idPeriodoAcademico, null);
    }
}
