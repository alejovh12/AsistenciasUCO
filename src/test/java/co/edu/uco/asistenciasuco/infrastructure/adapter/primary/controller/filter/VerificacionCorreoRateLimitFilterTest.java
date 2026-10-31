package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IDENT-001 BE-R01: limite de intentos en memoria por IP sobre el endpoint publico de verificacion. Solo cuenta
 * {@code remoteAddr}: un {@code X-Forwarded-For} falsificado no permite evadir el limite.
 */
class VerificacionCorreoRateLimitFilterTest {

    private static final String RUTA = "/api/v1/public/verificar-correo";

    private final AtomicReference<Instant> ahora = new AtomicReference<>(Instant.parse("2026-10-31T10:00:00Z"));
    private final Clock reloj = new Clock() {
        @Override
        public java.time.ZoneId getZone() {
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
    private final AtomicInteger pasaron = new AtomicInteger();
    private final FilterChain cadena = (request, response) -> pasaron.incrementAndGet();

    private VerificacionCorreoRateLimitFilter filtro(final int limite, final int maxClaves) {
        return new VerificacionCorreoRateLimitFilter(limite, maxClaves, reloj, new SecurityErrorResponseWriter());
    }

    private MockHttpServletResponse invocar(final VerificacionCorreoRateLimitFilter filtro, final String metodo, final String ruta,
                                            final String ip, final String forwardedFor) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRemoteAddr(ip);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, cadena);
        return response;
    }

    @Test
    void permite_hasta_el_limite_y_responde_429_a_la_siguiente_en_la_misma_ventana() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(3, 100);

        for (int i = 0; i < 3; i++) {
            assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());
        }
        final MockHttpServletResponse bloqueada = invocar(filtro, "POST", RUTA, "10.0.0.1", null);

        assertEquals(429, bloqueada.getStatus());
        assertTrue(bloqueada.getContentAsString().contains("TOO_MANY_REQUESTS"), bloqueada.getContentAsString());
        assertEquals(3, pasaron.get());
    }

    @Test
    void despues_de_la_ventana_vuelve_a_permitir() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(1, 100);
        assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());
        assertEquals(429, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());

        ahora.set(ahora.get().plusSeconds(61));

        assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());
    }

    @Test
    void cada_ip_tiene_su_propio_contador() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(1, 100);

        assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());
        assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.2", null).getStatus());
        assertEquals(429, invocar(filtro, "POST", RUTA, "10.0.0.1", null).getStatus());
    }

    @Test
    void ignora_x_forwarded_for_y_cuenta_la_ip_de_la_conexion() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(1, 100);

        assertEquals(200, invocar(filtro, "POST", RUTA, "10.0.0.1", "1.1.1.1").getStatus());
        assertEquals(429, invocar(filtro, "POST", RUTA, "10.0.0.1", "2.2.2.2").getStatus());
    }

    @Test
    void no_limita_otras_rutas_ni_otros_metodos() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(1, 100);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, invocar(filtro, "POST", "/api/v1/grupos", "10.0.0.1", null).getStatus());
            assertEquals(200, invocar(filtro, "GET", RUTA, "10.0.0.1", null).getStatus());
        }
        assertEquals(10, pasaron.get());
    }

    @Test
    void la_memoria_esta_acotada_a_un_maximo_de_claves() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(5, 3);

        for (int i = 1; i <= 50; i++) {
            invocar(filtro, "POST", RUTA, "10.0.1." + i, null);
        }

        assertTrue(filtro.clavesActivas() <= 3, "claves activas: " + filtro.clavesActivas());
    }

    @Test
    void rechaza_configuracion_invalida() {
        assertThrows(IllegalArgumentException.class, () -> filtro(0, 10));
        assertThrows(IllegalArgumentException.class, () -> filtro(5, 0));
    }
}
