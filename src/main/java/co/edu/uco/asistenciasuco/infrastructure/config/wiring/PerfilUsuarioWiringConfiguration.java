package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.ConsultarPerfilInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.interactor.ConsultarPerfilInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.ConsultarPerfilUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.impl.ConsultarPerfilUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.PerfilUsuarioQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Casos de uso POJO del perfil propio (HU006). Archivo propio para no tocar el wiring de usuarios e identidad. */
@Configuration(proxyBeanMethods = false)
public class PerfilUsuarioWiringConfiguration {

    @Bean ConsultarPerfilUseCase consultarPerfilUseCase(final PerfilUsuarioQueryPort port) {
        return new ConsultarPerfilUseCaseImpl(port);
    }

    @Bean ConsultarPerfilInputPort consultarPerfilInputPort(final ConsultarPerfilUseCase useCase) {
        return new ConsultarPerfilInteractor(useCase);
    }
}
