package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto;

import java.util.UUID;

public record ConsultarAlertaTempranaDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        String semaforo,
        String nombre,
        Integer pagina,
        Integer tamano,
        UUID idPrograma   // CULM-001 B2NM (S4): opcional; obligatorio de hecho si el coordinador tiene varios programas (COO_002)
) {
    public ConsultarAlertaTempranaDTO(final UUID actorUsuarioId, final UUID idPeriodoAcademico, final String semaforo, final String nombre, final Integer pagina, final Integer tamano) {
        this(actorUsuarioId, idPeriodoAcademico, semaforo, nombre, pagina, tamano, null);
    }
}
