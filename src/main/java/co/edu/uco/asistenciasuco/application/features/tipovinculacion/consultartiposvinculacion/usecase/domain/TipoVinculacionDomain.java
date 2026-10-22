package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.domain;

import java.util.UUID;

public record TipoVinculacionDomain(UUID id, String codigo, String nombre) {
}
