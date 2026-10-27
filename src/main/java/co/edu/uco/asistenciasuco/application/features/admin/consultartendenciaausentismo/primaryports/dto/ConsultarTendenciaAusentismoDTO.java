package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto;

import java.util.UUID;

public record ConsultarTendenciaAusentismoDTO(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        String granularidad
) {
}
