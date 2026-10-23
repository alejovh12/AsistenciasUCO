package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain;

import java.util.UUID;

public record TipoProgramaDomain(UUID id, String codigo, String nombre, boolean activo) {
}
