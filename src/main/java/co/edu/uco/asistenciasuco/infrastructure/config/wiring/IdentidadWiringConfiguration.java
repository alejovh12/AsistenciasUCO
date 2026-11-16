package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.AccesoCuentaService;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.ActivacionCuentaService;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.ReenviarActivacionCuentaService;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.primaryports.ReenviarActivacionCuentaInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.VerificacionCorreoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.CorreccionIdentidadService;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.drift.MetricsUsuarioSincronizacionIdentidadAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

/**
 * Composition Root de la sincronizacion de cuentas de identidad. Es independiente del proveedor activo
 * ({@code app.adapters.identity.provider}): solo depende de {@link IdentityProviderPort}.
 */
@Configuration(proxyBeanMethods = false)
public class IdentidadWiringConfiguration {

    @Bean
    UsuarioSincronizacionIdentidadPort usuarioSincronizacionIdentidadPort(final MeterRegistry meterRegistry) {
        return new MetricsUsuarioSincronizacionIdentidadAdapter(meterRegistry);
    }

    @Bean
    SincronizarCuentaInstitucionalService sincronizarCuentaInstitucionalService(
            final IdentityProviderPort identityProviderPort,
            final UsuarioSincronizacionIdentidadPort usuarioSincronizacionIdentidadPort
    ) {
        return new SincronizarCuentaInstitucionalService(identityProviderPort, usuarioSincronizacionIdentidadPort);
    }

    @Bean
    AsegurarCuentaIdentidadService asegurarCuentaIdentidadService(
            final IdentityProviderPort identityProviderPort,
            final UsuarioSincronizacionIdentidadPort usuarioSincronizacionIdentidadPort
    ) {
        return new AsegurarCuentaIdentidadService(identityProviderPort, usuarioSincronizacionIdentidadPort);
    }

    @Bean
    AccesoCuentaService accesoCuentaService(
            final InstitutionalScopePort institutionalScopePort,
            final IdentityProviderPort identityProviderPort,
            final AccessProfilePort accessProfilePort,
            final UsuarioSincronizacionIdentidadPort usuarioSincronizacionIdentidadPort,
            final ExternalIdentityLinkPort externalIdentityLinkPort
    ) {
        return new AccesoCuentaService(institutionalScopePort, identityProviderPort, accessProfilePort,
                usuarioSincronizacionIdentidadPort, externalIdentityLinkPort);
    }

    /** IDENT-002: piezas comunes de las altas sin contrasena (credencial legada inutilizable y aviso de activacion). */
    @Bean
    ActivacionCuentaService activacionCuentaService(
            final NotificacionPort notificacionPort,
            final PasswordEncoderPort passwordEncoderPort
    ) {
        return new ActivacionCuentaService(notificacionPort, passwordEncoderPort, new SecureRandom());
    }

    /** IDENT-002: reenvio del enlace de activacion por quien puede gestionar la cuenta. */
    @Bean
    ReenviarActivacionCuentaInputPort reenviarActivacionCuentaInputPort(
            final InstitutionalScopePort institutionalScopePort,
            final VerificacionCorreoRepositoryPort verificacionCorreoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final NotificacionPort notificacionPort,
            final MessageCatalogPort messageCatalogPort
    ) {
        return new ReenviarActivacionCuentaService(institutionalScopePort, verificacionCorreoRepositoryPort,
                usuarioRepositoryPort, notificacionPort, messageCatalogPort);
    }

    /** HU159: correccion de identidad de estudiantes (coordinador) y decanos (administrador). */
    @Bean
    CorreccionIdentidadService correccionIdentidadService(
            final CorreccionIdentidadCommandPort correccionIdentidadCommandPort,
            final SincronizarCuentaInstitucionalService sincronizarCuentaInstitucionalService,
            final MessageCatalogPort messageCatalogPort,
            final ParameterCatalogPort parameterCatalogPort
    ) {
        return new CorreccionIdentidadService(correccionIdentidadCommandPort, sincronizarCuentaInstitucionalService,
                messageCatalogPort, parameterCatalogPort);
    }
}
