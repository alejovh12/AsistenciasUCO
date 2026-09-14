package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de coordinadores.
 */
public record CoordinadorDomain(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                                 UUID idPrograma, String nombrePrograma, boolean estaActivoCoordinador) {
}
