package co.edu.uco.asistenciasuco.infrastructure.config.features;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.interactor.CrearUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl.CrearUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter.UsuarioRepositorySqlServerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class UsuarioBeansConfig {

    @Bean
    public UsuarioRepositoryPort usuarioRepositoryPort(final JdbcTemplate jdbcTemplate) {
        return new UsuarioRepositorySqlServerAdapter(jdbcTemplate);
    }

    @Bean
    public CrearUsuarioUseCase crearUsuarioUseCase(
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort
    ) {
        return new CrearUsuarioUseCaseImpl(usuarioRepositoryPort, passwordEncoderPort);
    }

    @Bean
    public CrearUsuarioInputPort crearUsuarioInputPort(final CrearUsuarioUseCase crearUsuarioUseCase) {
        return new CrearUsuarioInteractor(crearUsuarioUseCase);
    }
}
