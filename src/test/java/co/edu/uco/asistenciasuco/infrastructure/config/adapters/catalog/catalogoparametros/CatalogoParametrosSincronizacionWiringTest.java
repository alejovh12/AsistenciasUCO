package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarparametros.usecase.SincronizarParametrosCatalogoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.messaging.catalogoparametros.CatalogoParametrosEventListener;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosEventsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * INT-004: el contexto carga con el proveedor catalogoparametros (el arranque real fallo en INT-003 sin que lo viera ninguna prueba
 * unitaria): caso de uso, cliente de Pulsar, listener e indicador de salud, con la suscripcion y la reconciliacion por propiedad.
 */
class CatalogoParametrosSincronizacionWiringTest {

    private static final String[] BASE = {
            "app.adapters.parameter-catalog.provider=catalogoparametros",
            "app.providers.catalogo-parametros.base-url=http://localhost:8081/catalogo-parametros/api/v1",
            "app.providers.catalogo-parametros.token-uri=http://localhost:19090/realms/r/protocol/openid-connect/token",
            "app.providers.catalogo-parametros.client-id=svc-asistencias-dev",
            "app.providers.catalogo-parametros.client-secret=valor-secreto-de-prueba"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CatalogoParametrosAdapterConfiguration.class, CatalogoParametrosSincronizacionConfiguration.class)
            .withBean(ParametroCatalogoRepositoryPort.class, () -> mock(ParametroCatalogoRepositoryPort.class))
            .withBean(CatalogInvalidationPort.class, () -> mock(CatalogInvalidationPort.class))
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void con_los_eventos_apagados_solo_registra_el_caso_de_uso() {
        runner.withPropertyValues(BASE).withPropertyValues("app.providers.catalogo-parametros.events.enabled=false").run(context -> {
            assertNull(context.getStartupFailure());
            assertNotNull(context.getBean(SincronizarParametrosCatalogoUseCase.class));
            assertTrue(context.getBeansOfType(PulsarClient.class).isEmpty());
            assertTrue(context.getBeansOfType(CatalogoParametrosEventListener.class).isEmpty());
            assertTrue(context.getBeansOfType(CatalogoParametrosEventsHealthIndicator.class).isEmpty());
        });
    }

    @Test
    void con_los_eventos_activos_registra_cliente_listener_y_salud_aunque_no_haya_broker() {
        runner.withPropertyValues(BASE).withPropertyValues(
                        "app.providers.catalogo-parametros.events.service-url=pulsar://127.0.0.1:1",
                        "app.providers.catalogo-parametros.events.reconcile-on-startup=false")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(PulsarClient.class));
                    final var listener = context.getBean(CatalogoParametrosEventListener.class);
                    assertTrue(listener.isRunning());
                    final var salud = context.getBean(CatalogoParametrosEventsHealthIndicator.class).health();
                    // Sin broker no hay suscripcion: UNKNOWN, nunca DOWN (no debe tumbar el readiness).
                    assertEquals(Status.UNKNOWN, salud.getStatus());
                    assertFalse((Boolean) salud.getDetails().get("suscrito"));
                });
    }

    @Test
    void el_topic_por_defecto_se_deriva_del_client_id_y_uno_explicito_manda() {
        final var porDefecto = new CatalogoParametrosEventsProperties(null, null, null, null, null, null);
        assertEquals("persistent://public/default/catalogo-parametros-events-svc-1", porDefecto.topicFor("svc-1"));
        assertEquals("pulsar://localhost:6652", porDefecto.serviceUrl());
        assertEquals("asistencias-uco-backend-parametros", porDefecto.subscriptionName());
        assertTrue(porDefecto.enabled());
        assertTrue(porDefecto.reconcileOnStartup());

        final var explicito = new CatalogoParametrosEventsProperties(false, " pulsar+ssl://broker:6651 ", " persistent://t/n/x ", "p", "s", false);
        assertEquals("persistent://t/n/x", explicito.topicFor("svc-1"));
        assertEquals("pulsar+ssl://broker:6651", explicito.serviceUrl());
        assertFalse(explicito.enabled());
        assertFalse(explicito.reconcileOnStartup());
        assertThrows(IllegalStateException.class, () -> new CatalogoParametrosEventsProperties(true, "http://broker", null, null, null, null));
    }

    @Test
    void sin_secreto_de_la_identidad_el_contexto_falla() {
        runner.withPropertyValues(BASE).withPropertyValues(
                        "app.providers.catalogo-parametros.client-secret=",
                        "app.providers.catalogo-parametros.events.enabled=false")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void con_otro_provider_ni_la_configuracion_de_eventos_escaneada_sola_registra_algo() throws ClassNotFoundException {
        new ApplicationContextRunner()
                .withUserConfiguration(Class.forName(CatalogoParametrosSincronizacionConfiguration.class.getName() + "$Eventos"))
                .withPropertyValues("app.adapters.parameter-catalog.provider=sqlserver")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertTrue(context.getBeansOfType(PulsarClient.class).isEmpty());
                });
        runner.withPropertyValues("app.adapters.parameter-catalog.provider=sqlserver").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(SincronizarParametrosCatalogoUseCase.class).isEmpty());
        });
    }
}
