package co.edu.uco.asistenciasuco.application.features.decano.common.dto;

import java.util.UUID;

/**
 * Salida del puerto primario al editar o cambiar el estado de un coordinador.
 *
 * @param idCoordinador          coordinador afectado.
 * @param sincronizacionIdentidad {@code OK} o {@code PENDIENTE} (el cambio esta en DB, la cuenta se sincronizara).
 * @param mensajeUsuario         advertencia lista para mostrar cuando esta pendiente; nulo si todo OK.
 */
public record ResultadoGestionCoordinadorDTO(UUID idCoordinador, String sincronizacionIdentidad, String mensajeUsuario) {

    public static final String SINCRONIZACION_OK = "OK";
    public static final String SINCRONIZACION_PENDIENTE = "PENDIENTE";
}
