package co.edu.uco.asistenciasuco.application.features.decano.common.domain;

import java.util.UUID;

/**
 * Resultado de editar o cambiar el estado de un coordinador.
 *
 * @param idCoordinador           coordinador afectado.
 * @param sincronizacionPendiente {@code true} si el cambio quedo en DB pero la cuenta de identidad no pudo
 *                                sincronizarse todavia.
 * @param mensajeUsuario          advertencia del catalogo cuando la sincronizacion queda pendiente; nulo si todo OK.
 */
public record ResultadoGestionCoordinadorDomain(UUID idCoordinador, boolean sincronizacionPendiente, String mensajeUsuario) {
}
