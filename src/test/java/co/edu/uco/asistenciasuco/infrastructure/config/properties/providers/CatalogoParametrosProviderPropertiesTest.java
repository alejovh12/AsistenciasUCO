package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogoParametrosProviderPropertiesTest {

    private static final URI TOKEN = URI.create("http://localhost:19090/token");
    private static final String SECRETO = "valor-secreto-de-prueba";

    private static CatalogoParametrosProviderProperties con(final String baseUrl, final URI token, final String secreto,
                                                            final Duration timeout, final Duration ttl,
                                                            final String separador) {
        return new CatalogoParametrosProviderProperties(baseUrl, token, "svc-asistencias", secreto, null, null, timeout, ttl, separador);
    }

    @Test
    void aplica_valores_por_defecto() {
        final var p = con("http://h/api/v1", TOKEN, SECRETO, null, null, null);

        assertEquals(Duration.ofSeconds(10), p.timeout());
        assertEquals(Duration.ofMinutes(5), p.cacheTtl());
        assertEquals(".", p.separador());
    }

    @Test
    void separador_vacio_usa_el_defecto_y_uno_explicito_se_conserva() {
        assertEquals(".", con("http://h", TOKEN, SECRETO, null, null, "").separador());
        assertEquals(":", con("http://h", TOKEN, SECRETO, null, null, ":").separador());
    }

    @Test
    void quita_barras_finales_y_espacios_de_la_url_base() {
        assertEquals("http://h/api/v1", con("  http://h/api/v1//  ", TOKEN, SECRETO, null, null, null).baseUrl());
    }

    @Test
    void permite_cache_ttl_cero() {
        assertEquals(Duration.ZERO, con("http://h", TOKEN, SECRETO, null, Duration.ZERO, null).cacheTtl());
    }

    @Test
    void rechaza_base_url_vacia_nula_o_solo_barras() {
        assertThrows(IllegalStateException.class, () -> con(null, TOKEN, SECRETO, null, null, null));
        assertThrows(IllegalStateException.class, () -> con("  ", TOKEN, SECRETO, null, null, null));
        assertThrows(IllegalStateException.class, () -> con("///", TOKEN, SECRETO, null, null, null));
    }

    @Test
    void rechaza_token_uri_nula() {
        final var e = assertThrows(IllegalStateException.class, () -> con("http://h", null, SECRETO, null, null, null));
        assertEquals("app.providers.catalogo-parametros.token-uri es obligatorio.", e.getMessage());
    }

    @Test
    void rechaza_client_id_vacio() {
        final var e = assertThrows(IllegalStateException.class, () -> new CatalogoParametrosProviderProperties(
                "http://h", TOKEN, " ", SECRETO, null, null, null, null, null));
        assertEquals("app.providers.catalogo-parametros.client-id es obligatorio.", e.getMessage());
    }

    @Test
    void rechaza_client_secret_vacio_sin_exponer_valores() {
        final var e = assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, " ", null, null, null));
        assertEquals("app.providers.catalogo-parametros.client-secret es obligatorio.", e.getMessage());
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, null, null, null, null));
    }

    @Test
    void rechaza_timeout_no_positivo_y_ttl_negativo() {
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, Duration.ZERO, null, null));
        final var duration2 = Duration.ofSeconds(-1);
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, duration2, null, null));
        final var duration = Duration.ofSeconds(-1);
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, null, duration, null));
    }

    @Test
    void to_string_no_contiene_el_secreto() {
        assertFalse(con("http://h", TOKEN, SECRETO, null, null, null).toString().contains(SECRETO));
    }
}
