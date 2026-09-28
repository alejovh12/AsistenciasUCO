package co.edu.uco.asistenciasuco.infrastructure.config.adapters.realtime.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root del provider realtime local basado en Reactor + SSE.
 *
 * <p>La seleccion de tecnologia se resuelve exclusivamente con
 * {@code app.adapters.realtime.provider=local-sse}; ni Application ni los controllers conocen
 * esa propiedad. El adapter concreto no se autoregistra con estereotipos Spring.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "app.adapters.realtime",
        name = "provider",
        havingValue = "local-sse",
        matchIfMissing = true
)
public class LocalSseRealtimeAdapterConfiguration {

    @Bean
    public ReactorRealtimeAdapter reactorRealtimeAdapter(final MeterRegistry meterRegistry) {
        return new ReactorRealtimeAdapter(meterRegistry);
    }

    @Bean
    public RealtimeStreamGateway realtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new LocalSseRealtimeStreamGateway(realtimeAdapter, institutionalScopePort);
    }
}
