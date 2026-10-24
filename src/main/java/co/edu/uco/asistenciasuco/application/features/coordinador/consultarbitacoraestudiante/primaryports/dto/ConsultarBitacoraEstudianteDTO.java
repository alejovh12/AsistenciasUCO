package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto;

import java.util.UUID;

public record ConsultarBitacoraEstudianteDTO(
        UUID actorUsuarioId,
        UUID idEstudiante,
        UUID idPeriodoAcademico,
        Integer pagina,
        Integer tamano
) {
}
