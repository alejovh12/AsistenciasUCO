package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain;

import java.util.UUID;

public record ConsultarFichaEstudianteDomain(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico,
        UUID idPrograma   // CULM-001 B2NM (S4): opcional; obligatorio de hecho si el coordinador tiene varios programas (COO_002)
) {
    public ConsultarFichaEstudianteDomain(final UUID actorUsuarioId, final UUID idEstudiante, final UUID idPeriodoAcademico) {
        this(actorUsuarioId, idEstudiante, idPeriodoAcademico, null);
    }
}
