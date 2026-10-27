package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain;

import java.util.UUID;

public record ConsultarFichaDocenteDomain(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idPeriodoAcademico,
        Integer pagina,
        Integer tamano
) {
}
