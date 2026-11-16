package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * INT-003: avisos de cambio de MessageUcoLab 0.3.0 por Pulsar. MessageUcoLab publica en el topic de cada identidad de
 * servicio ({@code <topicPrefix>-<clientId>}) solo los cambios del ambiente asociado a esa identidad.
 *
 * <p>Solo aplica con {@code app.adapters.message-catalog.provider=messageucolab}. Si Pulsar no responde, el backend
 * arranca igual: la suscripcion es asincrona y los mensajes se siguen leyendo con su cache.</p>
 *
 * @param enabled            activa la suscripcion a los avisos.
 * @param serviceUrl         broker de Pulsar ({@code pulsar://localhost:6800} desde el PC; {@code pulsar://pulsar:6650}
 *                           desde la red de Docker de MessageUcoLab).
 * @param topicPrefix        prefijo del topic; MessageUcoLab agrega {@code -<clientId>}.
 * @param subscriptionName   nombre de la suscripcion durable del backend (tipo Failover: una instancia procesa).
 * @param reconcileOnStartup al arrancar se copian en la BD todos los textos vigentes (cubre avisos perdidos o emitidos con
 *                           el backend apagado; MessageUcoLab publica con mejor esfuerzo).
 */
@ConfigurationProperties(prefix = "app.providers.messageucolab.events")
public record MessageUcoLabEventsProperties(
        Boolean enabled,
        String serviceUrl,
        String topicPrefix,
        String subscriptionName,
        Boolean reconcileOnStartup
) {

    public MessageUcoLabEventsProperties {
        enabled = enabled == null || enabled;
        serviceUrl = blank(serviceUrl) ? "pulsar://localhost:6800" : serviceUrl.trim();
        topicPrefix = blank(topicPrefix) ? "persistent://public/default/message-events" : topicPrefix.trim();
        subscriptionName = blank(subscriptionName) ? "asistencias-uco-backend" : subscriptionName.trim();
        reconcileOnStartup = reconcileOnStartup == null || reconcileOnStartup;
        if (!serviceUrl.startsWith("pulsar://") && !serviceUrl.startsWith("pulsar+ssl://")) {
            throw new IllegalStateException("app.providers.messageucolab.events.service-url debe empezar por pulsar:// o pulsar+ssl://.");
        }
    }

    /** Topic de la identidad de servicio: {@code <topicPrefix>-<clientId>}. */
    public String topicFor(final String clientId) {
        return topicPrefix + "-" + clientId;
    }

    private static boolean blank(final String value) {
        return value == null || value.isBlank();
    }
}
