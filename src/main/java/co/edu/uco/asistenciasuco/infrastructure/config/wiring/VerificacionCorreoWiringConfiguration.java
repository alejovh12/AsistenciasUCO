package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.ConsultarConfiguracionRegistroEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.interactor.ConsultarConfiguracionRegistroEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.ConsultarConfiguracionRegistroEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.impl.ConsultarConfiguracionRegistroEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.ReenviarVerificacionCorreoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.interactor.ReenviarVerificacionCorreoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.ReenviarVerificacionCorreoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.impl.ReenviarVerificacionCorreoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.VerificarCorreoInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.interactor.VerificarCorreoInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.VerificarCorreoUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.impl.VerificarCorreoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.VerificacionCorreoRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;

/**
 * IDENT-001: composition root de la verificacion de correo, el reenvio del enlace, la configuracion del registro manual y
 * el limite de intentos de la unica ruta publica nueva.
 */
@Configuration(proxyBeanMethods = false)
public class VerificacionCorreoWiringConfiguration {

    @Bean
    public VerificarCorreoUseCase verificarCorreoUseCase(
            final VerificacionCorreoRepositoryPort verificacionCorreoRepositoryPort,
            final IdentityProviderPort identityProviderPort,
            final MessageCatalogPort messageCatalogPort
    ) {
        return new VerificarCorreoUseCaseImpl(verificacionCorreoRepositoryPort, identityProviderPort, messageCatalogPort);
    }

    @Bean
    public VerificarCorreoInputPort verificarCorreoInputPort(final VerificarCorreoUseCase verificarCorreoUseCase) {
        return new VerificarCorreoInteractor(verificarCorreoUseCase);
    }

    @Bean
    public ReenviarVerificacionCorreoUseCase reenviarVerificacionCorreoUseCase(
            final VerificacionCorreoRepositoryPort verificacionCorreoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final NotificacionPort notificacionPort
    ) {
        return new ReenviarVerificacionCorreoUseCaseImpl(verificacionCorreoRepositoryPort, usuarioRepositoryPort, notificacionPort);
    }

    @Bean
    public ReenviarVerificacionCorreoInputPort reenviarVerificacionCorreoInputPort(
            final ReenviarVerificacionCorreoUseCase reenviarVerificacionCorreoUseCase
    ) {
        return new ReenviarVerificacionCorreoInteractor(reenviarVerificacionCorreoUseCase);
    }

    @Bean
    public ConsultarConfiguracionRegistroEstudianteUseCase consultarConfiguracionRegistroEstudianteUseCase(
            final CorreoInstitucionalPolicy correoInstitucionalPolicy
    ) {
        return new ConsultarConfiguracionRegistroEstudianteUseCaseImpl(correoInstitucionalPolicy);
    }

    @Bean
    public ConsultarConfiguracionRegistroEstudianteInputPort consultarConfiguracionRegistroEstudianteInputPort(
            final ConsultarConfiguracionRegistroEstudianteUseCase consultarConfiguracionRegistroEstudianteUseCase
    ) {
        return new ConsultarConfiguracionRegistroEstudianteInteractor(consultarConfiguracionRegistroEstudianteUseCase);
    }

    /** Se registra antes de la cadena de Spring Security para limitar tambien los intentos que terminan en 401 o 400. */
    @Bean
    public FilterRegistrationBean<VerificacionCorreoRateLimitFilter> verificacionCorreoRateLimitFilter(
            @Value("${app.verificacion-correo.limite-por-minuto:20}") final int limitePorMinuto,
            @Value("${app.verificacion-correo.max-claves:10000}") final int maxClaves,
            final SecurityErrorResponseWriter securityErrorResponseWriter
    ) {
        final FilterRegistrationBean<VerificacionCorreoRateLimitFilter> registro = new FilterRegistrationBean<>(
                new VerificacionCorreoRateLimitFilter(limitePorMinuto, maxClaves, Clock.systemUTC(), securityErrorResponseWriter));
        registro.addUrlPatterns(VerificacionCorreoRateLimitFilter.RUTA);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registro;
    }
}
