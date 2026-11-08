package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto;

import java.util.UUID;

public record ConsultarFichaEstudianteDTO(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico,
        UUID idPrograma   // CULM-001 B2NM (S4): opcional; obligatorio de hecho si el coordinador tiene varios programas (COO_002)
) {
    public ConsultarFichaEstudianteDTO(final UUID actorUsuarioId, final UUID idEstudiante, final UUID idPeriodoAcademico) {
        this(actorUsuarioId, idEstudiante, idPeriodoAcademico, null);
    }
}
