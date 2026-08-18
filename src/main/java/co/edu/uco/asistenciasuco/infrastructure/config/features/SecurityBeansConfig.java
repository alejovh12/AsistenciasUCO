package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.secondaryports.security.AuthenticationRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.AuthenticationRepositorySqlServerAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SpringPasswordEncoderAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoderPort passwordEncoderPort() {
        return new SpringPasswordEncoderAdapter();
    }

    @Bean
    public AuthenticationRepositoryPort authenticationRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new AuthenticationRepositorySqlServerAdapter(jdbcTemplate);
    }
}
