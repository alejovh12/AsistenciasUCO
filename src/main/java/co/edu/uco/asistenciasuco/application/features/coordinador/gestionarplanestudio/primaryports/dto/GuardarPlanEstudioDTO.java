package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto;

import java.util.UUID;

public record GuardarPlanEstudioDTO(UUID idPlanEstudio, Integer inp, UUID usuario) {
}
