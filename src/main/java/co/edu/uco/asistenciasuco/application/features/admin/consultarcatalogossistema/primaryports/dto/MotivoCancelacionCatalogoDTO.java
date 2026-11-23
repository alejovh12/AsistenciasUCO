package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto;

import java.util.UUID;

/** Motivo de cancelacion de sesion definido por el sistema (solo lectura). */
public record MotivoCancelacionCatalogoDTO(UUID id, String codigo, String nombre, int orden, boolean estaActivo) {
}
