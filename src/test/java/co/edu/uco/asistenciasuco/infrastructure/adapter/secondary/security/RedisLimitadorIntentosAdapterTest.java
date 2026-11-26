package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Code review 2026-11-26: el limitador Redis usa una ventana deslizante atomica (script Lua), con la misma semantica que
 * el limitador en memoria; la prueba con Redis real esta en {@code RedisRealIntegrationTest}.
 */
class RedisLimitadorIntentosAdapterTest {

    private static final Instant AHORA = Instant.parse("2026-11-25T12:00:30Z");
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final String CLAVE = "asistencia-qr:pin:123456";

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LimitadorIntentosPort respaldo = mock(LimitadorIntentosPort.class);
    private RedisLimitadorIntentosAdapter limitador;
    private String claveRedis;

    @BeforeEach
    void preparar() {
        limitador = new RedisLimitadorIntentosAdapter(redis, "asistencias-uco", respaldo, RELOJ);
        claveRedis = "asistencias-uco:limite:v2:" + RedisLimitadorIntentosAdapter.hash(CLAVE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void ejecuta_el_script_atomico_con_ventana_de_60_segundos_y_el_limite() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class))).thenReturn(1L);
        final ArgumentCaptor<List<String>> claves = ArgumentCaptor.forClass(List.class);
        final ArgumentCaptor<Object> argumentos = ArgumentCaptor.forClass(Object.class);

        assertTrue(limitador.permitir(CLAVE, 3));

        verify(redis).execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), claves.capture(), argumentos.capture(),
                argumentos.capture(), argumentos.capture(), argumentos.capture());
        assertEquals(List.of(claveRedis), claves.getValue());
        assertEquals(Long.toString(AHORA.toEpochMilli()), argumentos.getAllValues().get(0));
        assertEquals("60000", argumentos.getAllValues().get(1));
        assertEquals("3", argumentos.getAllValues().get(2));
    }

    @Test
    void el_script_poda_cuenta_registra_y_fija_el_vencimiento_en_una_sola_operacion() {
        final String script = RedisLimitadorIntentosAdapter.SCRIPT.getScriptAsString();
        assertTrue(script.contains("ZREMRANGEBYSCORE"));
        assertTrue(script.contains("ZCARD"));
        assertTrue(script.contains("ZADD"));
        assertTrue(script.contains("PEXPIRE"), "la clave siempre queda con vencimiento");
    }

    @Test
    void rechaza_cuando_el_script_rechaza() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class))).thenReturn(0L);

        assertFalse(limitador.permitir(CLAVE, 3));
    }

    @SuppressWarnings("unchecked")
    @Test
    void un_limite_menor_que_uno_se_trata_como_uno() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class))).thenReturn(1L);
        final ArgumentCaptor<Object> argumentos = ArgumentCaptor.forClass(Object.class);

        limitador.permitir(CLAVE, 0);

        verify(redis).execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), any(List.class), argumentos.capture(),
                argumentos.capture(), argumentos.capture(), argumentos.capture());
        assertEquals("1", argumentos.getAllValues().get(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void cada_intento_usa_un_miembro_distinto() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class))).thenReturn(1L);
        final ArgumentCaptor<Object> argumentos = ArgumentCaptor.forClass(Object.class);

        limitador.permitir(CLAVE, 3);
        limitador.permitir(CLAVE, 3);

        verify(redis, times(2)).execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), any(List.class), argumentos.capture(),
                argumentos.capture(), argumentos.capture(), argumentos.capture());
        assertNotEquals(argumentos.getAllValues().get(3), argumentos.getAllValues().get(7),
                "dos intentos en el mismo milisegundo cuentan como dos");
    }

    @Test
    void con_redis_caido_decide_el_limitador_local() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("caido"));
        when(respaldo.permitir(CLAVE, 3)).thenReturn(false);

        assertFalse(limitador.permitir(CLAVE, 3));
    }

    @Test
    void sin_respuesta_de_redis_decide_el_limitador_local() {
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class))).thenReturn(null);
        when(respaldo.permitir(CLAVE, 3)).thenReturn(true);

        assertTrue(limitador.permitir(CLAVE, 3));
    }

    @Test
    void la_clave_se_guarda_como_hash() {
        final String hash = RedisLimitadorIntentosAdapter.hash(CLAVE);
        assertEquals(32, hash.length());
        assertFalse(claveRedis.contains("123456"));
    }

    @Test
    void con_el_disyuntor_abierto_no_llama_a_redis() {
        final co.edu.uco.asistenciasuco.infrastructure.cache.DisyuntorRedis disyuntor =
                new co.edu.uco.asistenciasuco.infrastructure.cache.DisyuntorRedis(RELOJ);
        final RedisLimitadorIntentosAdapter conDisyuntor = new RedisLimitadorIntentosAdapter(redis, "asistencias-uco",
                respaldo, RELOJ, disyuntor);
        when(redis.execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("caido"));
        when(respaldo.permitir(CLAVE, 3)).thenReturn(true);

        assertTrue(conDisyuntor.permitir(CLAVE, 3));
        assertTrue(conDisyuntor.permitir(CLAVE, 3));

        verify(redis, times(1)).execute(eq(RedisLimitadorIntentosAdapter.SCRIPT), anyList(), any(Object[].class));
        verify(respaldo, times(2)).permitir(CLAVE, 3);
    }
}
