package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security;

import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisLimitadorIntentosAdapterTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-25T12:00:30Z"), ZoneOffset.UTC);
    private static final long MINUTO = Instant.parse("2026-11-25T12:00:30Z").toEpochMilli() / 60_000L;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valores = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final LimitadorIntentosPort respaldo = mock(LimitadorIntentosPort.class);
    private RedisLimitadorIntentosAdapter limitador;
    private String clave;

    @BeforeEach
    void preparar() {
        when(redis.opsForValue()).thenReturn(valores);
        limitador = new RedisLimitadorIntentosAdapter(redis, "asistencias-uco", respaldo, RELOJ);
        clave = "asistencias-uco:limite:v1:" + RedisLimitadorIntentosAdapter.hash("asistencia-qr:pin:123456") + ":" + MINUTO;
    }

    @Test
    void el_primer_intento_fija_el_vencimiento_de_la_ventana() {
        when(valores.increment(clave)).thenReturn(1L);

        assertTrue(limitador.permitir("asistencia-qr:pin:123456", 3));
        verify(redis).expire(clave, RedisLimitadorIntentosAdapter.VENCIMIENTO_VENTANA);
    }

    @Test
    void permite_hasta_el_limite_y_rechaza_despues() {
        when(valores.increment(clave)).thenReturn(2L, 3L, 4L);

        assertTrue(limitador.permitir("asistencia-qr:pin:123456", 3));
        assertTrue(limitador.permitir("asistencia-qr:pin:123456", 3));
        assertFalse(limitador.permitir("asistencia-qr:pin:123456", 3));
        verify(redis, never()).expire(anyString(), org.mockito.ArgumentMatchers.any(java.time.Duration.class));
    }

    @Test
    void un_limite_menor_que_uno_se_trata_como_uno() {
        when(valores.increment(clave)).thenReturn(1L, 2L);

        assertTrue(limitador.permitir("asistencia-qr:pin:123456", 0));
        assertFalse(limitador.permitir("asistencia-qr:pin:123456", 0));
    }

    @Test
    void con_redis_caido_decide_el_limitador_local() {
        when(valores.increment(clave)).thenThrow(new RedisConnectionFailureException("caido"));
        when(respaldo.permitir("asistencia-qr:pin:123456", 3)).thenReturn(false);

        assertFalse(limitador.permitir("asistencia-qr:pin:123456", 3));
    }

    @Test
    void sin_respuesta_de_redis_decide_el_limitador_local() {
        when(valores.increment(clave)).thenReturn(null);
        when(respaldo.permitir("asistencia-qr:pin:123456", 3)).thenReturn(true);

        assertTrue(limitador.permitir("asistencia-qr:pin:123456", 3));
    }

    @Test
    void la_clave_se_guarda_como_hash() {
        final String hash = RedisLimitadorIntentosAdapter.hash("asistencia-qr:pin:123456");
        assertEquals(32, hash.length());
        assertFalse(clave.contains("123456"));
    }
}
