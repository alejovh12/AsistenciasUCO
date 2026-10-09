package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion de guardar (crear/actualizar) plan de estudio.
 */
public record PlanEstudioDomain(UUID idPlanEstudio, Integer inp, UUID usuario) {
}
