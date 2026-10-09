package co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        IdentityAdapterProperties.class,
        SecurityAdapterProperties.class,
        StorageAdapterProperties.class,
        RealtimeAdapterProperties.class,
        AuditAdapterProperties.class
})
public class AdapterPropertiesConfiguration {
}
