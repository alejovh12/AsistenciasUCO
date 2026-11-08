package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain;

import java.util.UUID;

public record ConsultarBitacoraEstudianteDomain(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico,
        Integer pagina,
        Integer tamano,
        UUID idPrograma   // CULM-001 B2NM (S4): opcional; obligatorio de hecho si el coordinador tiene varios programas (COO_002)
) {
    public ConsultarBitacoraEstudianteDomain(final UUID actorUsuarioId, final UUID idEstudiante, final UUID idPeriodoAcademico, final Integer pagina, final Integer tamano) {
        this(actorUsuarioId, idEstudiante, idPeriodoAcademico, pagina, tamano, null);
    }
}
