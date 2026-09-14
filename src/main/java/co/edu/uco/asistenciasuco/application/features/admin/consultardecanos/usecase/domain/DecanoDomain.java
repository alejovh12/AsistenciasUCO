package co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de decanos.
 */
public record DecanoDomain(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                            UUID idFacultad, String nombreFacultad, boolean estaActivoDecano) {
}
