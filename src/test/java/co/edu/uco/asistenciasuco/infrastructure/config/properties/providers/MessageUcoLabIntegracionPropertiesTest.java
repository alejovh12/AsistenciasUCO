package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** INT-003: validacion de la identidad de servicio y de los avisos de MessageUcoLab 0.3.0. */
class MessageUcoLabIntegracionPropertiesTest {

    private static final URI TOKEN = URI.create("http://localhost:19090/realms/security-baseline/protocol/openid-connect/token");

    @Test
    void identidad_completa_con_timeout_por_defecto_y_sin_secreto_en_to_string() {
        final MessageUcoLabServiceIdentityProperties identidad =
                new MessageUcoLabServiceIdentityProperties(TOKEN, " svc-1 ", "secreto-real", null);
        assertEquals("svc-1", identidad.clientId());
        assertEquals(Duration.ofSeconds(10), identidad.requestTimeout());
        assertFalse(identidad.toString().contains("secreto-real"));
        assertTrue(identidad.toString().contains("svc-1"));
    }

    @Test
    void identidad_incompleta_falla_al_arrancar() {
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(null, "svc", "s", null));
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(TOKEN, null, "s", null));
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(TOKEN, " ", "s", null));
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(TOKEN, "svc", null, null));
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(TOKEN, "svc", "", null));
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabServiceIdentityProperties(TOKEN, "svc", "s", Duration.ZERO));
        final Duration negativa = Duration.ofSeconds(-1);
        assertThrows(IllegalStateException.class,
                () -> new MessageUcoLabServiceIdentityProperties(TOKEN, "svc", "s", negativa));
    }

    @Test
    void eventos_con_valores_por_defecto_del_kit() {
        final MessageUcoLabEventsProperties eventos = new MessageUcoLabEventsProperties(null, null, null, null, null);
        assertTrue(eventos.enabled());
        assertTrue(eventos.reconcileOnStartup());
        assertEquals("pulsar://localhost:6800", eventos.serviceUrl());
        assertEquals("asistencias-uco-backend", eventos.subscriptionName());
        assertEquals("persistent://public/default/message-events-svc-1", eventos.topicFor("svc-1"));
    }

    @Test
    void eventos_respetan_lo_configurado_y_exigen_url_de_pulsar() {
        final MessageUcoLabEventsProperties eventos = new MessageUcoLabEventsProperties(false, " pulsar+ssl://broker:6651 ",
                " persistent://t/n/avisos ", " otra ", false);
        assertFalse(eventos.enabled());
        assertFalse(eventos.reconcileOnStartup());
        assertEquals("pulsar+ssl://broker:6651", eventos.serviceUrl());
        assertEquals("persistent://t/n/avisos-x", eventos.topicFor("x"));
        assertEquals("otra", eventos.subscriptionName());
        assertThrows(IllegalStateException.class, () -> new MessageUcoLabEventsProperties(true, "http://broker", null, null, null));
        assertEquals("pulsar://localhost:6800", new MessageUcoLabEventsProperties(true, " ", " ", " ", true).serviceUrl());
    }
}
