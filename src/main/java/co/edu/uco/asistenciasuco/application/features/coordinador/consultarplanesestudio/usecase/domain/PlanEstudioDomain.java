package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de planes de estudio del coordinador.
 */
public record PlanEstudioDomain(
        UUID id,
        UUID idPrograma,
        String nombrePrograma,
        String inp,
        boolean estaActivoPlanEstudio,
        String estaActivoTextoPlanEstudio,
        String justificacionEstado
) {
}
