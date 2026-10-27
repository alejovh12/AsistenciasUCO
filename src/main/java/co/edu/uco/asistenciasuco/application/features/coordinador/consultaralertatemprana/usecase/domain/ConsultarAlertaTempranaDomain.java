package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain;

import java.util.UUID;

public record ConsultarAlertaTempranaDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        String semaforo,
        String nombre,
        Integer pagina,
        Integer tamano
) {
}
