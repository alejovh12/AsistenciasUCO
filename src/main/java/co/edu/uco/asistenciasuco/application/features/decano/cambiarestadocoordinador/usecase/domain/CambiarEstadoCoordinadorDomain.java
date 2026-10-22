package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion activar o desactivar un coordinador (HU122). El estado destino es explicito.
 */
public record CambiarEstadoCoordinadorDomain(UUID idCoordinador, boolean estaActivo, UUID usuario) {
}
