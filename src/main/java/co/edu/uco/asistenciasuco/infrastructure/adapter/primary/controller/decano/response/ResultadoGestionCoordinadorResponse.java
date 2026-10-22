package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.response;

import java.util.UUID;

/**
 * Respuesta de editar o cambiar el estado de un coordinador. {@code mensajeUsuario} solo trae valor cuando la
 * sincronizacion de la cuenta queda pendiente (advertencia del catalogo); el sobre {@code ApiDataResponse} no
 * tiene campo de mensaje.
 */
public record ResultadoGestionCoordinadorResponse(UUID idCoordinador, String sincronizacionIdentidad, String mensajeUsuario) {
}
