package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.RestablecerClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.SolicitarRecuperacionClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.interactor.RestablecerClaveInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.interactor.SolicitarRecuperacionClaveInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.RestablecerClaveUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.SolicitarRecuperacionClaveUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl.RestablecerClaveUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl.SolicitarRecuperacionClaveUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.VerificacionCorreoRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPublicEndpoints;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;
import java.util.Set;

/** HU177: composition root de la recuperacion de contrasena y del limite de intentos de sus dos rutas publicas. */
@Configuration(proxyBeanMethods = false)
public class RecuperacionClaveWiringConfiguration {

    @Bean
    public SolicitarRecuperacionClaveUseCase solicitarRecuperacionClaveUseCase(
            final RecuperacionClaveRepositoryPort recuperacionClaveRepositoryPort,
            final NotificacionPort notificacionPort,
            final MessageCatalogPort messageCatalogPort
    ) {
        return new SolicitarRecuperacionClaveUseCaseImpl(recuperacionClaveRepositoryPort, notificacionPort, messageCatalogPort);
    }

    @Bean
    public SolicitarRecuperacionClaveInputPort solicitarRecuperacionClaveInputPort(
            final SolicitarRecuperacionClaveUseCase solicitarRecuperacionClaveUseCase
    ) {
        return new SolicitarRecuperacionClaveInteractor(solicitarRecuperacionClaveUseCase);
    }

    @Bean
    public RestablecerClaveUseCase restablecerClaveUseCase(
            final RecuperacionClaveRepositoryPort recuperacionClaveRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final IdentityProviderPort identityProviderPort,
            final ExternalIdentityLinkPort externalIdentityLinkPort,
            final MessageCatalogPort messageCatalogPort
    ) {
        return new RestablecerClaveUseCaseImpl(
                recuperacionClaveRepositoryPort, usuarioRepositoryPort, identityProviderPort, externalIdentityLinkPort,
                messageCatalogPort);
    }

    @Bean
    public RestablecerClaveInputPort restablecerClaveInputPort(final RestablecerClaveUseCase restablecerClaveUseCase) {
        return new RestablecerClaveInteractor(restablecerClaveUseCase);
    }

    /**
     * Limite por IP de las dos rutas publicas, mas estricto que el de la verificacion de correo porque pedir el enlace
     * dispara un correo. Se registra antes de la cadena de Spring Security para contar tambien los intentos rechazados.
     */
    @Bean
    public FilterRegistrationBean<VerificacionCorreoRateLimitFilter> recuperacionClaveRateLimitFilter(
            @Value("${app.recuperacion-clave.limite-por-minuto:5}") final int limitePorMinuto,
            @Value("${app.recuperacion-clave.max-claves:10000}") final int maxClaves,
            final SecurityErrorResponseWriter securityErrorResponseWriter
    ) {
        final FilterRegistrationBean<VerificacionCorreoRateLimitFilter> registro = new FilterRegistrationBean<>(
                new VerificacionCorreoRateLimitFilter(
                        limitePorMinuto, maxClaves, Clock.systemUTC(), securityErrorResponseWriter,
                        Set.of(SecurityPublicEndpoints.RECUPERACION_CLAVE_SOLICITUD,
                                SecurityPublicEndpoints.RECUPERACION_CLAVE_CONFIRMACION)));
        registro.addUrlPatterns(
                SecurityPublicEndpoints.RECUPERACION_CLAVE_SOLICITUD,
                SecurityPublicEndpoints.RECUPERACION_CLAVE_CONFIRMACION);
        registro.setName("recuperacionClaveRateLimitFilter");
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }
}
