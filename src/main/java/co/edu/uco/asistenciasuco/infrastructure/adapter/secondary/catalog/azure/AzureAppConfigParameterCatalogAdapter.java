package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para Azure App Configuration.
 * Implementa {@link ParameterCatalogPort} utilizando el {@link ConfigurationClient} de Azure SDK.
 *
 * <p>Incorpora una cache concurrente en memoria (ConcurrentHashMap) para optimizar
 * lecturas y respetar la cuota de peticiones del tier gratuito (F0).</p>
 *
 * <p>No se autoregistra con anotaciones de Spring (@Component/@Service). Es instanciado
 * y configurado exclusivamente por el Composition Root.</p>
 */
public class AzureAppConfigParameterCatalogAdapter implements ParameterCatalogPort {

    private final ConfigurationClient client;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AzureAppConfigParameterCatalogAdapter(final ConfigurationClient client) {
        this.client = Objects.requireNonNull(client, "ConfigurationClient de Azure App Configuration es obligatorio.");
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        final String normalizedKey = buildKey(group, key);

        // 1. Revisar cache en memoria
        if (cache.containsKey(normalizedKey)) {
            return Optional.ofNullable(cache.get(normalizedKey));
        }

        // 2. Consultar en Azure App Configuration si no esta en cache
        try {
            final ConfigurationSetting setting = client.getConfigurationSetting(normalizedKey, null);
            if (setting != null && setting.getValue() != null) {
                cache.put(normalizedKey, setting.getValue());
                return Optional.of(setting.getValue());
            }
            return Optional.empty();
        } catch (final ResourceNotFoundException e) {
            // Intentar con formato alternativo grupo/clave o clave simple si aplica
            return fallbackSearch(group, key, normalizedKey);
        } catch (final Exception e) {
            throw new ParameterCatalogException("Error al consultar el parametro [" + normalizedKey + "] en Azure App Configuration.", e);
        }
    }

    private Optional<String> fallbackSearch(final String group, final String key, final String originalNormalizedKey) {
        try {
            // Intento 2: formato plano "grupo:clave"
            final String colonKey = group + ":" + key;
            if (!colonKey.equals(originalNormalizedKey)) {
                final ConfigurationSetting setting = client.getConfigurationSetting(colonKey, null);
                if (setting != null && setting.getValue() != null) {
                    cache.put(originalNormalizedKey, setting.getValue());
                    return Optional.of(setting.getValue());
                }
            }
        } catch (final ResourceNotFoundException ignored) {
            // No existe
        }
        return Optional.empty();
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key)
                .orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String rawValue = getRequiredParameter(group, key);
        if (targetType == String.class) {
            return (T) rawValue;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(rawValue.trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(rawValue.trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(rawValue.trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(rawValue.trim());
        }
        throw new IllegalArgumentException("Tipo destino no soportado para conversion directa: " + targetType.getName());
    }

    /**
     * Invalida la cache local en memoria.
     */
    public void clearCache() {
        cache.clear();
    }

    private String buildKey(final String group, final String key) {
        // En Azure App Configuration la jerarquia comun usa delimitador ':' o '/'
        return group.trim() + ":" + key.trim();
    }
}
