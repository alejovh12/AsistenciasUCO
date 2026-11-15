package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.interactor.ConsultarMensajeInterfazInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.ConsultarMensajeInterfazUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.impl.ConsultarMensajeInterfazUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.interactor.ResolverMensajeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.impl.ResolverMensajeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.MensajesInterfazRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;

/**
 * Composition Root de los casos de uso del catalogo de mensajes: resolucion de mensajes de usuario y, desde INT-003, los
 * textos de la interfaz que la SPA lee por la ruta publica (con su limite de peticiones por IP).
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

    @Bean
    public ConsultarMensajeInterfazUseCase consultarMensajeInterfazUseCase(final MessageCatalogPort messageCatalogPort) {
        return new ConsultarMensajeInterfazUseCaseImpl(messageCatalogPort);
    }

    @Bean
    public ConsultarMensajeInterfazInputPort consultarMensajeInterfazInputPort(final ConsultarMensajeInterfazUseCase useCase) {
        return new ConsultarMensajeInterfazInteractor(useCase);
    }

    /** Antes de la cadena de Spring Security, como el limite de la verificacion de correo. */
    @Bean
    public FilterRegistrationBean<MensajesInterfazRateLimitFilter> mensajesInterfazRateLimitFilter(
            @Value("${app.mensajes-interfaz.limite-por-minuto:240}") final int limitePorMinuto,
            @Value("${app.mensajes-interfaz.max-claves:10000}") final int maxClaves,
            final SecurityErrorResponseWriter securityErrorResponseWriter
    ) {
        final FilterRegistrationBean<MensajesInterfazRateLimitFilter> registro = new FilterRegistrationBean<>(
                new MensajesInterfazRateLimitFilter(limitePorMinuto, maxClaves, Clock.systemUTC(), securityErrorResponseWriter));
        registro.addUrlPatterns(MensajesInterfazRateLimitFilter.RUTA_BASE + "/*");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }
}
