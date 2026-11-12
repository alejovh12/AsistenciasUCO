package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogoParametrosProviderPropertiesTest {

    private static final UUID TOKEN = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SECRETO = "valor-secreto-de-prueba";

    private static CatalogoParametrosProviderProperties con(final String baseUrl, final UUID token, final String secreto,
                                                            final Duration timeout, final Duration ttl,
                                                            final String separador) {
        return new CatalogoParametrosProviderProperties(baseUrl, token, secreto, null, null, timeout, ttl, separador);
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
    void rechaza_token_id_nulo() {
        final var e = assertThrows(IllegalStateException.class, () -> con("http://h", null, SECRETO, null, null, null));
        assertEquals("app.providers.catalogo-parametros.token-id es obligatorio.", e.getMessage());
    }

    @Test
    void rechaza_secreto_vacio_sin_exponer_valores() {
        final var e = assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, " ", null, null, null));
        assertEquals("app.providers.catalogo-parametros.secreto es obligatorio.", e.getMessage());
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, null, null, null, null));
    }

    @Test
    void rechaza_timeout_no_positivo_y_ttl_negativo() {
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, Duration.ZERO, null, null));
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, Duration.ofSeconds(-1), null, null));
        assertThrows(IllegalStateException.class, () -> con("http://h", TOKEN, SECRETO, null, Duration.ofSeconds(-1), null));
    }

    @Test
    void to_string_no_contiene_el_secreto() {
        assertFalse(con("http://h", TOKEN, SECRETO, null, null, null).toString().contains(SECRETO));
    }
}
