package co.edu.uco.asistenciasuco.infrastructure.config.adapters.vault.local;

import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.vault.local.LocalEnvSecretVaultAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para el proveedor local de contingencia (variables de entorno / propiedades de sistema).
 *
 * <p>Se activa cuando {@code app.adapters.vault.provider=local_env}.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.vault",
        name = "provider",
        havingValue = "local_env"
)
public class LocalEnvVaultAdapterConfiguration {

    @Bean
    public SecretVaultPort secretVaultPort() {
        return new LocalEnvSecretVaultAdapter();
    }
}
