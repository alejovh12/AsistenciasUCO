package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.util.UUID;

public record PlanEstudioDTO(UUID id, UUID idPrograma, String nombrePrograma, String inp,
                             boolean estaActivoPlanEstudio, String estaActivoTextoPlanEstudio,
                             String justificacionEstado) {
}
