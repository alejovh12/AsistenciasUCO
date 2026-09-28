package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.interactor.ResolverMensajeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.impl.ResolverMensajeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root del caso de uso de resolucion de mensajes de usuario.
 *
 * <p>El provider de {@link MessageCatalogPort} lo selecciona la configuracion de adapters de catalogo.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CatalogoWiringConfiguration {

    @Bean
    public ResolverMensajeUsuarioUseCase resolverMensajeUsuarioUseCase(final MessageCatalogPort messageCatalogPort) {
        return new ResolverMensajeUsuarioUseCaseImpl(messageCatalogPort);
    }

    @Bean
    public ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort(final ResolverMensajeUsuarioUseCase useCase) {
        return new ResolverMensajeUsuarioInteractor(useCase);
    }
}
