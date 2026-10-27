package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain;

import java.util.UUID;

public record ConsultarBitacoraEstudianteDomain(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico,
        Integer pagina,
        Integer tamano
) {
}
