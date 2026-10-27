package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain;

import java.util.UUID;

public record ConsultarTendenciaAusentismoDomain(
        UUID actorUsuarioId,
        UUID idPeriodoAcademico,
        String granularidad
) {
}
