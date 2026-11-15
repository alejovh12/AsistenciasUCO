package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MessageUcoLabServiceIdentityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INT-003: token client_credentials de la identidad de servicio para MessageUcoLab, con cache hasta 30 s antes de vencer. */
class MessageUcoLabServiceIdentityTokenProviderTest {

    private static final URI TOKEN_URI = URI.create("http://localhost:19090/realms/security-baseline/protocol/openid-connect/token");
    private final MessageUcoLabServiceIdentityProperties identidad =
            new MessageUcoLabServiceIdentityProperties(TOKEN_URI, "svc-123-asistencias-dev", "s3cr&to", Duration.ofSeconds(5));
    private final HttpClient http = mock(HttpClient.class);
    private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-11-15T00:00:00Z"));
    private final Clock reloj = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora.get();
        }
    };
    private final MessageUcoLabServiceIdentityTokenProvider provider =
            new MessageUcoLabServiceIdentityTokenProvider(identidad, http, JsonMapper.builder().build(), reloj);

    @SuppressWarnings("unchecked")
    private void responde(final int status, final String body) throws Exception {
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test
    void pide_el_token_con_client_credentials_y_lo_reutiliza_hasta_30s_antes_de_vencer() throws Exception {
        responde(200, "{\"access_token\":\"tok-1\",\"expires_in\":300}");

        assertEquals("tok-1", provider.token().block());
        ahora.set(ahora.get().plusSeconds(260));
        assertEquals("tok-1", provider.token().block());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        ahora.set(ahora.get().plusSeconds(20));
        provider.token().block();
        final ArgumentCaptor<HttpRequest> pedido = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(2)).send(pedido.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals(TOKEN_URI, pedido.getValue().uri());
        assertEquals("POST", pedido.getValue().method());
        final String form = cuerpo(pedido.getValue());
        assertTrue(form.contains("grant_type=client_credentials"));
        assertTrue(form.contains("client_id=svc-123-asistencias-dev"));
        assertTrue(form.contains("client_secret=s3cr%26to"));
    }

    @Test
    void sin_expires_in_usa_la_vigencia_por_defecto_e_invalidar_fuerza_un_token_nuevo() throws Exception {
        responde(200, "{\"access_token\":\"tok-2\"}");
        assertEquals("tok-2", provider.tokenVigente());
        provider.invalidar();
        provider.tokenVigente();
        verify(http, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void error_http_o_respuesta_sin_token_fallan_con_mensaje_claro() throws Exception {
        responde(401, "{}");
        assertTrue(assertThrows(IllegalStateException.class, provider::tokenVigente).getMessage().contains("HTTP 401"));

        responde(200, "{\"token_type\":\"Bearer\"}");
        assertTrue(assertThrows(IllegalStateException.class, provider::tokenVigente).getMessage().contains("access_token"));

        responde(200, "{\"access_token\":\" \"}");
        assertThrows(IllegalStateException.class, provider::tokenVigente);
    }

    @Test
    void el_mono_propaga_el_error() throws Exception {
        responde(500, "{}");
        assertThrows(IllegalStateException.class, () -> provider.token().block());
    }

    @Test
    void dependencias_obligatorias() {
        final JsonMapper mapper = JsonMapper.builder().build();
        assertThrows(NullPointerException.class, () -> new MessageUcoLabServiceIdentityTokenProvider(null, http, mapper, reloj));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabServiceIdentityTokenProvider(identidad, null, mapper, reloj));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabServiceIdentityTokenProvider(identidad, http, null, reloj));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabServiceIdentityTokenProvider(identidad, http, mapper, null));
    }

    private static String cuerpo(final HttpRequest request) {
        final StringBuilder texto = new StringBuilder();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final ByteBuffer item) {
                texto.append(StandardCharsets.UTF_8.decode(item));
            }

            @Override
            public void onError(final Throwable throwable) {
                throw new IllegalStateException(throwable);
            }

            @Override
            public void onComplete() {
                // nada
            }
        });
        return texto.toString();
    }
}
