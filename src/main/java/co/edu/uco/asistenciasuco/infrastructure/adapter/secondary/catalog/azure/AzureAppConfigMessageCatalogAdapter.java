package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para resolver mensajes desde Azure App Configuration.
 * Implementa {@link MessageCatalogPort} utilizando el {@link ConfigurationClient} de Azure SDK.
 *
 * <p>Convención de claves:
 * <ul>
 *   <li>Mensajes de usuario: clave {@code messages:user:<CODIGO>} y label {@code es}.</li>
 *   <li>Mensajes técnicos: clave {@code messages:technical:<CODIGO>} (sin label o null).</li>
 * </ul>
 * </p>
 *
 * <p>Incorpora una caché concurrente en memoria para evitar el consumo recurrente de cuota
 * en la nube.</p>
 */
public class AzureAppConfigMessageCatalogAdapter implements MessageCatalogPort {

    private static final String LABEL_ES = "es";
    private static final String USER_PREFIX = "messages:user:";
    private static final String TECHNICAL_PREFIX = "messages:technical:";

    private final ConfigurationClient client;
    private final Map<String, String> userMessageCache = new ConcurrentHashMap<>();
    private final Map<String, String> technicalMessageCache = new ConcurrentHashMap<>();

    public AzureAppConfigMessageCatalogAdapter(final ConfigurationClient client) {
        this.client = Objects.requireNonNull(client, "ConfigurationClient de Azure App Configuration es obligatorio.");
    }

    @Override
    public String getUserMessage(final String code, final Object... args) {
        final String template = findUserMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public String getTechnicalMessage(final String code, final Object... args) {
        final String template = findTechnicalMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public Optional<String> findUserMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        if (userMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(userMessageCache.get(cleanCode));
        }

        final String key = USER_PREFIX + cleanCode;
        try {
            // Consulta con label 'es'
            ConfigurationSetting setting = null;
            try {
                setting = client.getConfigurationSetting(key, LABEL_ES);
            } catch (final ResourceNotFoundException notFoundWithLabel) {
                // Fallback sin label si no se especifico
                try {
                    setting = client.getConfigurationSetting(key, null);
                } catch (final ResourceNotFoundException ignored) {
                    // No existe en Azure
                }
            }

            if (setting != null && setting.getValue() != null) {
                userMessageCache.put(cleanCode, setting.getValue());
                return Optional.of(setting.getValue());
            }
            return Optional.empty();
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje de usuario en Azure App Configuration para código: " + cleanCode, e);
        }
    }

    public Optional<String> findTechnicalMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        if (technicalMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(technicalMessageCache.get(cleanCode));
        }

        final String key = TECHNICAL_PREFIX + cleanCode;
        try {
            final ConfigurationSetting setting = client.getConfigurationSetting(key, null);
            if (setting != null && setting.getValue() != null) {
                technicalMessageCache.put(cleanCode, setting.getValue());
                return Optional.of(setting.getValue());
            }
            return Optional.empty();
        } catch (final ResourceNotFoundException e) {
            return Optional.empty();
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje técnico en Azure App Configuration para código: " + cleanCode, e);
        }
    }

    public void clearCache() {
        userMessageCache.clear();
        technicalMessageCache.clear();
    }

    private String formatMessage(final String template, final Object... args) {
        if (args == null || args.length == 0 || template == null || template.isBlank()) {
            return template;
        }

        // Manejar plantillas con {} o con {0}, {1}
        String normalized = template;
        if (normalized.contains("{}")) {
            for (final Object arg : args) {
                normalized = normalized.replaceFirst("\\{\\}", arg != null ? arg.toString() : "null");
            }
            return normalized;
        }

        try {
            return MessageFormat.format(normalized, args);
        } catch (final Exception e) {
            return normalized;
        }
    }
}
