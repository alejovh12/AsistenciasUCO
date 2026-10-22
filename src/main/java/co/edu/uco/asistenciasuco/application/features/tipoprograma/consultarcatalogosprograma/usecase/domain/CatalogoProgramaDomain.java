package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.domain;

import java.util.UUID;

public record CatalogoProgramaDomain(UUID id, String codigo, String nombre) {
}
