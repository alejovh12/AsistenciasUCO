package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.drift.MetricsUsuarioSincronizacionIdentidadAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
