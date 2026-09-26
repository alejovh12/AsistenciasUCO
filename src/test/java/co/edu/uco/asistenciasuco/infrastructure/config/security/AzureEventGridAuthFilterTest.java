package co.edu.uco.asistenciasuco.infrastructure.config.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contrato del filtro de autenticación del webhook Event Grid (SEC-AZ-001..004): la credencial
 * viaja únicamente en el header {@code aeg-sas-token}, nunca por query string, y un filtro sin
 * credencial configurada rechaza (fail-closed). Los valores son literales TEST_ONLY.
 */
class AzureEventGridAuthFilterTest {

    private static final String TEST_ONLY_WEBHOOK_TOKEN = "unit-test-webhook-token";
    private static final String WEBHOOK_URI = "/api/v1/internal/azure-events";

    private static MockHttpServletRequest webhookRequest() {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setRequestURI(WEBHOOK_URI);
        return request;
    }

    private static Outcome run(final String configuredToken, final MockHttpServletRequest request)
            throws ServletException, IOException {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();
        new AzureEventGridAuthFilter(configuredToken).doFilter(request, response, chain);
        return new Outcome(response, chain.getRequest() != null);
    }

    private record Outcome(MockHttpServletResponse response, boolean chainContinued) {
        int status() {
            return response.getStatus();
        }
    }

    // ---- Otros endpoints: el filtro no interviene -------------------------------------------

    @Test
    void permite_otros_endpoints_sin_validar_token() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/sesiones");
        request.setRequestURI("/api/v1/sesiones");

        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

        assertEquals(true, outcome.chainContinued());
        assertEquals(200, outcome.status());
    }

    // ---- SEC-AZ-001: sin credencial configurada => fail-closed ------------------------------

    @Test
    void rechaza_con_401_si_no_hay_credencial_configurada_aunque_llegue_header() throws Exception {
        for (final String configured : new String[]{null, "", "   ", "\t\n"}) {
            final MockHttpServletRequest request = webhookRequest();
            request.addHeader("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN);

            final Outcome outcome = run(configured, request);

            assertEquals(401, outcome.status(), "configured=" + configured);
            assertEquals(false, outcome.chainContinued(), "configured=" + configured);
        }
    }

    @Test
    void rechaza_con_401_si_no_hay_credencial_configurada_y_llega_header_vacio() throws Exception {
        final MockHttpServletRequest request = webhookRequest();
        request.addHeader("aeg-sas-token", "");

        final Outcome outcome = run("", request);

        assertEquals(401, outcome.status());
        assertEquals(false, outcome.chainContinued());
    }

    // ---- SEC-AZ-002: la credencial por query string está prohibida --------------------------

    @Test
    void rechaza_con_401_si_la_credencial_correcta_llega_solo_por_query_string() throws Exception {
        for (final String parameter : new String[]{"token", "access_token", "key", "aeg-sas-token"}) {
            final MockHttpServletRequest request = webhookRequest();
            request.setParameter(parameter, TEST_ONLY_WEBHOOK_TOKEN);

            final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

            assertEquals(401, outcome.status(), "parameter=" + parameter);
            assertEquals(false, outcome.chainContinued(), "parameter=" + parameter);
        }
    }

    @Test
    void ignora_el_query_string_aunque_el_header_sea_incorrecto() throws Exception {
        final MockHttpServletRequest request = webhookRequest();
        request.addHeader("aeg-sas-token", "wrong-test-only-token");
        request.setParameter("token", TEST_ONLY_WEBHOOK_TOKEN);

        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

        assertEquals(401, outcome.status());
        assertEquals(false, outcome.chainContinued());
    }

    // ---- SEC-AZ-003: header ausente / vacío / incorrecto ------------------------------------

    @Test
    void rechaza_con_401_si_falta_el_header() throws Exception {
        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, webhookRequest());

        assertEquals(401, outcome.status());
        assertEquals(false, outcome.chainContinued());
    }

    @Test
    void rechaza_con_401_si_el_header_esta_vacio_o_en_blanco() throws Exception {
        for (final String value : new String[]{"", "   "}) {
            final MockHttpServletRequest request = webhookRequest();
            request.addHeader("aeg-sas-token", value);

            final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

            assertEquals(401, outcome.status(), "value='" + value + "'");
            assertEquals(false, outcome.chainContinued(), "value='" + value + "'");
        }
    }

    @Test
    void rechaza_con_401_si_el_header_es_incorrecto() throws Exception {
        for (final String value : new String[]{"wrong-test-only-token", "unit-test-webhook-toke", TEST_ONLY_WEBHOOK_TOKEN + "x"}) {
            final MockHttpServletRequest request = webhookRequest();
            request.addHeader("aeg-sas-token", value);

            final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

            assertEquals(401, outcome.status(), "value=" + value);
            assertEquals(false, outcome.chainContinued(), "value=" + value);
        }
    }

    // ---- SEC-AZ-004: header correcto continúa la cadena -------------------------------------

    @Test
    void permite_acceso_cuando_header_aeg_sas_token_es_valido() throws Exception {
        final MockHttpServletRequest request = webhookRequest();
        request.addHeader("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN);

        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);

        assertEquals(true, outcome.chainContinued());
        assertEquals(200, outcome.status());
    }

    @Test
    void tolera_espacios_alrededor_de_la_credencial_configurada_y_del_header() throws Exception {
        final MockHttpServletRequest request = webhookRequest();
        request.addHeader("aeg-sas-token", "  " + TEST_ONLY_WEBHOOK_TOKEN + " ");

        final Outcome outcome = run(" " + TEST_ONLY_WEBHOOK_TOKEN + "  ", request);

        assertEquals(true, outcome.chainContinued());
    }

    // ---- Path matching: solo el endpoint inequívoco (con context path soportado) -------------

    @Test
    void no_intercepta_rutas_arbitrarias_que_solo_terminan_igual() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/otro/api/v1/internal/azure-events");
        request.setRequestURI("/otro/api/v1/internal/azure-events");

        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);
        assertEquals(true, outcome.chainContinued());
        assertEquals(200, outcome.status());
    }

    @Test
    void soporta_context_path_configurado() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/app" + WEBHOOK_URI);
        request.setContextPath("/app");
        request.setRequestURI("/app" + WEBHOOK_URI);

        final Outcome outcome = run(TEST_ONLY_WEBHOOK_TOKEN, request);
        assertEquals(401, outcome.status());
        assertEquals(false, outcome.chainContinued());
    }
}
