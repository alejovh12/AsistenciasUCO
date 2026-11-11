package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPublicEndpoints;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU177: el limite por IP cubre las dos rutas publicas de recuperacion de contrasena con un contador comun y no toca
 * otras rutas.
 */
class RecuperacionClaveRateLimitFilterTest {

    private static final String SOLICITUD = SecurityPublicEndpoints.RECUPERACION_CLAVE_SOLICITUD;
    private static final String CONFIRMACION = SecurityPublicEndpoints.RECUPERACION_CLAVE_CONFIRMACION;

    private final AtomicInteger pasaron = new AtomicInteger();
    private final FilterChain cadena = (request, response) -> pasaron.incrementAndGet();

    private static VerificacionCorreoRateLimitFilter filtro(final int limite) {
        return new VerificacionCorreoRateLimitFilter(
                limite, 100, Clock.systemUTC(), new SecurityErrorResponseWriter(), Set.of(SOLICITUD, CONFIRMACION));
    }

    private int invocar(final VerificacionCorreoRateLimitFilter filtro, final String metodo, final String ruta, final String ip)
            throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRemoteAddr(ip);
        final MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, cadena);
        return response.getStatus();
    }

    @Test
    void las_dos_rutas_comparten_el_contador_por_ip() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(2);

        assertEquals(200, invocar(filtro, "POST", SOLICITUD, "10.0.0.1"));
        assertEquals(200, invocar(filtro, "POST", CONFIRMACION, "10.0.0.1"));
        assertEquals(429, invocar(filtro, "POST", SOLICITUD, "10.0.0.1"));
        assertEquals(429, invocar(filtro, "POST", CONFIRMACION, "10.0.0.1"));
        assertEquals(200, invocar(filtro, "POST", SOLICITUD, "10.0.0.2"), "Otra IP tiene su propio contador.");
        assertEquals(3, pasaron.get());
    }

    @Test
    void no_limita_otras_rutas_ni_otros_metodos() throws Exception {
        final VerificacionCorreoRateLimitFilter filtro = filtro(1);

        assertEquals(200, invocar(filtro, "POST", SOLICITUD, "10.0.0.1"));
        for (int i = 0; i < 3; i++) {
            assertEquals(200, invocar(filtro, "POST", VerificacionCorreoRateLimitFilter.RUTA, "10.0.0.1"));
            assertEquals(200, invocar(filtro, "OPTIONS", SOLICITUD, "10.0.0.1"));
        }
        assertEquals(429, invocar(filtro, "POST", CONFIRMACION, "10.0.0.1"));
    }

    @Test
    void exige_al_menos_una_ruta() {
        final SecurityErrorResponseWriter writer = new SecurityErrorResponseWriter();
        final Clock reloj = Clock.systemUTC();

        assertThrows(IllegalArgumentException.class, () -> new VerificacionCorreoRateLimitFilter(1, 1, reloj, writer, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new VerificacionCorreoRateLimitFilter(1, 1, reloj, writer, null));
    }
}
