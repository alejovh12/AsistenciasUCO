package co.edu.uco.asistenciasuco.infrastructure.config.adapters.realtime.localsse;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LocalSseRealtimeAdapterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(LocalSseRealtimeAdapterConfiguration.class)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void provider_local_sse_registra_un_publisher_y_un_gateway() {
        contextRunner
                .withPropertyValues("app.adapters.realtime.provider=local-sse")
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(RealtimePublisherPort.class).size());
                    assertEquals(1, context.getBeansOfType(RealtimeStreamGateway.class).size());
                    assertInstanceOf(
                            ReactorRealtimeAdapter.class,
                            context.getBean(RealtimePublisherPort.class)
                    );
                });
    }

    @Test
    void provider_local_sse_es_el_default_compatible_cuando_no_se_declara_propiedad() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(RealtimePublisherPort.class).size());
            assertEquals(1, context.getBeansOfType(RealtimeStreamGateway.class).size());
        });
    }

    @Test
    void provider_distinto_no_autoregistra_el_adapter_local() {
        contextRunner
                .withPropertyValues("app.adapters.realtime.provider=otro")
                .run(context -> {
                    assertEquals(0, context.getBeansOfType(RealtimePublisherPort.class).size());
                    assertEquals(0, context.getBeansOfType(RealtimeStreamGateway.class).size());
                });
    }
}
