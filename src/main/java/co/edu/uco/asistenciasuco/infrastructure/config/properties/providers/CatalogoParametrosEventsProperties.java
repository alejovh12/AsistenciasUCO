package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * INT-004: eventos de cambio del Catalogo de Parametros por Pulsar. El Catalogo publica en un topic por identidad de servicio
 * ({@code <topicPrefix>-<clientId>}) solo los cambios de la aplicacion y el ambiente asociados a esa identidad.
 *
 * <p>Solo aplica con {@code app.adapters.parameter-catalog.provider=catalogoparametros}. Si Pulsar no responde, el backend
 * arranca igual: la suscripcion es asincrona y los parametros se siguen leyendo con su cache (y la BD).</p>
 *
 * @param enabled            activa la suscripcion y la reconciliacion de arranque (bandera de la funcion completa).
 * @param serviceUrl         broker de Pulsar del Catalogo ({@code pulsar://localhost:6652} desde el PC;
 *                           {@code pulsar://pulsar-parametros:6650} desde su red de Docker).
 * @param topic              topic completo; vacio = {@code <topicPrefix>-<clientId>}.
 * @param topicPrefix        prefijo del topic de la identidad.
 * @param subscriptionName   nombre de la suscripcion durable del backend (tipo Shared).
 * @param reconcileOnStartup al arrancar se copian en la BD todos los valores vigentes (una sola lectura M2M, sin sondeo).
 */
@ConfigurationProperties(prefix = "app.providers.catalogo-parametros.events")
public record CatalogoParametrosEventsProperties(
        Boolean enabled,
        String serviceUrl,
        String topic,
        String topicPrefix,
        String subscriptionName,
        Boolean reconcileOnStartup
) {

    public CatalogoParametrosEventsProperties {
        enabled = enabled == null || enabled;
        serviceUrl = blank(serviceUrl) ? "pulsar://localhost:6652" : serviceUrl.trim();
        topic = blank(topic) ? null : topic.trim();
        topicPrefix = blank(topicPrefix) ? "persistent://public/default/catalogo-parametros-events" : topicPrefix.trim();
        subscriptionName = blank(subscriptionName) ? "asistencias-uco-backend-parametros" : subscriptionName.trim();
        reconcileOnStartup = reconcileOnStartup == null || reconcileOnStartup;
        if (!serviceUrl.startsWith("pulsar://") && !serviceUrl.startsWith("pulsar+ssl://")) {
            throw new IllegalStateException("app.providers.catalogo-parametros.events.service-url debe empezar por pulsar:// o pulsar+ssl://.");
        }
    }

    /** Topic de la identidad de servicio: el configurado o {@code <topicPrefix>-<clientId>}. */
    public String topicFor(final String clientId) {
        return topic != null ? topic : topicPrefix + "-" + clientId;
    }

    private static boolean blank(final String value) {
        return value == null || value.isBlank();
    }
}
