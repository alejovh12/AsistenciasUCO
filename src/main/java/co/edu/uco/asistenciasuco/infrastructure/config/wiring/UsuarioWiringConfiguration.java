package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.CrearUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.primaryports.interactor.CrearUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.impl.CrearUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.ProvisionarUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.primaryports.interactor.ProvisionarUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.ProvisionarUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.impl.ProvisionarUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UsuarioWiringConfiguration {

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

    @Bean
    public ProvisionarUsuarioUseCase provisionarUsuarioUseCase(
            final CrearUsuarioUseCase crearUsuarioUseCase,
            final IdentityProviderPort identityProviderPort
    ) {
        return new ProvisionarUsuarioUseCaseImpl(crearUsuarioUseCase, identityProviderPort);
    }

    @Bean
    public ProvisionarUsuarioInputPort provisionarUsuarioInputPort(final ProvisionarUsuarioUseCase provisionarUsuarioUseCase) {
        return new ProvisionarUsuarioInteractor(provisionarUsuarioUseCase);
    }
}
