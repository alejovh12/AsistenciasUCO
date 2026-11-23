package co.edu.uco.notificaciones.infrastructure.web;

import co.edu.uco.notificaciones.infrastructure.config.NotificacionesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltrosWebTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    private ApiKeyFilter filtro(final String clave) {
        return new ApiKeyFilter(new NotificacionesProperties(clave, "log", new NotificacionesProperties.Smtp("", 587, "", "", true, false, "", "n",
                java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1), 50, 60, 500, "", new NotificacionesProperties.TestMode(false, "")),
                null), json);
    }

    private MockHttpServletResponse invocar(final ApiKeyFilter filtro, final String ruta, final String clave, final MockFilterChain cadena) throws Exception {
        final MockHttpServletRequest peticion = new MockHttpServletRequest("GET", ruta);
        peticion.setRequestURI(ruta);
        if (clave != null) {
            peticion.addHeader(ApiKeyFilter.CABECERA, clave);
        }
        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, cadena);
        return respuesta;
    }

    @Test
    void conLaClaveCorrectaContinua() throws Exception {
        final MockFilterChain cadena = new MockFilterChain();
        final MockHttpServletResponse r = invocar(filtro("secreta"), "/api/v1/notificaciones", "secreta", cadena);
        assertEquals(200, r.getStatus());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void sinClaveOConClaveIncorrectaResponde401ConCuerpoUniforme() throws Exception {
        MDC.put(CorrelationFilter.MDC_CLAVE, "corr-9");
        for (final String clave : new String[]{null, "", "otra", "secreta "}) {
            final MockFilterChain cadena = new MockFilterChain();
            final MockHttpServletResponse r = invocar(filtro("secreta"), "/api/v1/notificaciones", clave, cadena);
            assertEquals(401, r.getStatus());
            assertNull(cadena.getRequest());
            assertTrue(r.getContentType().startsWith("application/json"));
            final var cuerpo = json.readTree(r.getContentAsString());
            assertEquals("NO_AUTORIZADO", cuerpo.get("code").asString());
            assertEquals("corr-9", cuerpo.get("correlationId").asString());
        }
    }

    @Test
    void lasSondasDeSaludSonAbiertasPeroPrometheusNo() throws Exception {
        final ApiKeyFilter filtro = filtro("secreta");
        for (final String ruta : new String[]{"/actuator/health", "/actuator/health/readiness", "/actuator/info"}) {
            final MockFilterChain cadena = new MockFilterChain();
            assertEquals(200, invocar(filtro, ruta, null, cadena).getStatus());
            assertNotNull(cadena.getRequest(), ruta);
        }
        assertEquals(401, invocar(filtro, "/actuator/prometheus", null, new MockFilterChain()).getStatus());
        assertEquals(200, invocar(filtro, "/actuator/prometheus", "secreta", new MockFilterChain()).getStatus());
    }

    @Test
    void sinClaveConfiguradaNoArranca() {
        assertThrows(IllegalStateException.class, () -> filtro(" "));
        assertThrows(IllegalStateException.class, () -> filtro(null));
    }

    @Test
    void correlacionReutilizaElIdValidoYGeneraUnoNuevoSiNo() throws Exception {
        final CorrelationFilter filtro = new CorrelationFilter();
        final AtomicReference<String> dentro = new AtomicReference<>();
        final MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/v1/x");
        peticion.addHeader(CorrelationFilter.CABECERA, " abc-123 ");
        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, (req, res) -> dentro.set(MDC.get(CorrelationFilter.MDC_CLAVE)));
        assertEquals("abc-123", dentro.get());
        assertEquals("abc-123", respuesta.getHeader(CorrelationFilter.CABECERA));
        assertNull(MDC.get(CorrelationFilter.MDC_CLAVE));

        for (final String invalido : new String[]{"con espacio\ninyeccion", "x".repeat(65)}) {
            final MockHttpServletRequest otra = new MockHttpServletRequest("GET", "/api/v1/x");
            otra.addHeader(CorrelationFilter.CABECERA, invalido);
            final MockHttpServletResponse r2 = new MockHttpServletResponse();
            filtro.doFilter(otra, r2, (req, res) -> dentro.set(MDC.get(CorrelationFilter.MDC_CLAVE)));
            assertEquals(36, dentro.get().length());
            assertEquals(dentro.get(), r2.getHeader(CorrelationFilter.CABECERA));
        }

        final MockHttpServletRequest sin = new MockHttpServletRequest("GET", "/api/v1/x");
        final MockHttpServletResponse r3 = new MockHttpServletResponse();
        filtro.doFilter(sin, r3, (req, res) -> dentro.set(MDC.get(CorrelationFilter.MDC_CLAVE)));
        assertEquals(36, dentro.get().length());
    }
}
