package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto;

import java.util.UUID;

public record ConsultarAlertaTempranaDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        String semaforo,
        String nombre,
        Integer pagina,
        Integer tamano
) {
}
