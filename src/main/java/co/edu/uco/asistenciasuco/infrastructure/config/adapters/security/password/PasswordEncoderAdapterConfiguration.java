package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.password;

import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SpringPasswordEncoderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root para la capability de hashing de contraseñas.
 *
 * <p>Deliberadamente independiente de {@code app.adapters.security} (que selecciona el
 * proveedor OAuth/JWT): el hashing de contraseñas es una capability distinta que no
 * depende de qué IdP se use.</p>
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderAdapterConfiguration {

    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new SpringPasswordEncoderAdapter();
    }
}
