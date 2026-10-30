package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import java.util.Optional;

/**
 * Puerto interno de consulta de plantillas activas de notificacion.
 */
public interface PlantillaNotificacionRepository {

    Optional<PlantillaNotificacion> consultarActiva(String tipo, String idioma);
}
