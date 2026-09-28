package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.local;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de contingencia offline / local para resolver secretos desde variables de entorno
 * o propiedades del sistema.
 *
 * <p>Permite operar el sistema en desarrollo local desconectado o en entornos CI sin acceso
 * a Azure Key Vault, manteniendo el principio de sustituibilidad tecnica.</p>
 */
public class LocalEnvSecretVaultAdapter implements SecretVaultPort {

    private final Map<String, String> environmentOverrides;

    public LocalEnvSecretVaultAdapter() {
        this(Map.of());
    }

    public LocalEnvSecretVaultAdapter(final Map<String, String> environmentOverrides) {
        this.environmentOverrides = environmentOverrides != null ? environmentOverrides : Map.of();
    }

    @Override
    public Optional<String> getSecret(final String secretName) {
        if (secretName == null || secretName.isBlank()) {
            return Optional.empty();
        }

        // 1. Buscar en mapa de overrides locales
        if (environmentOverrides.containsKey(secretName)) {
            return Optional.ofNullable(environmentOverrides.get(secretName));
        }

        // 2. Buscar como propiedad del sistema (-Dsecret.name o -DSECRET_NAME)
        final String sysProp = System.getProperty(secretName);
        if (sysProp != null && !sysProp.isBlank()) {
            return Optional.of(sysProp);
        }

        // 3. Buscar en variables de entorno (transformando a formato ENV: database-password -> DATABASE_PASSWORD)
        final String envKey = secretName.replace('-', '_').replace('.', '_').toUpperCase(Locale.ROOT);
        final String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return Optional.of(envValue);
        }

        // 4. Intentar variable de entorno con el nombre literal exacto
        final String exactEnv = System.getenv(secretName);
        if (exactEnv != null && !exactEnv.isBlank()) {
            return Optional.of(exactEnv);
        }

        return Optional.empty();
    }

    @Override
    public String getRequiredSecret(final String secretName) {
        return getSecret(secretName)
                .orElseThrow(() -> new SecretNotFoundException(secretName));
    }
}
