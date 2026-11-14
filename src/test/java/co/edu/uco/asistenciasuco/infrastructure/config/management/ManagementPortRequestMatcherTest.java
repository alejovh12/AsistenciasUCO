package co.edu.uco.asistenciasuco.infrastructure.config.management;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VAL H2: la cadena abierta de gestion solo aplica a peticiones que entran por el puerto de gestion real, y nunca cuando ese
 * puerto no existe o coincide con el de la API.
 */
class ManagementPortRequestMatcherTest {

    private static boolean coincide(final Integer gestion, final Integer api, final int puertoPeticion) {
        final MockEnvironment env = new MockEnvironment();
        if (gestion != null) {
            env.setProperty(ManagementPortRequestMatcher.LOCAL_MANAGEMENT_PORT, String.valueOf(gestion));
        }
        if (api != null) {
            env.setProperty(ManagementPortRequestMatcher.LOCAL_SERVER_PORT, String.valueOf(api));
        }
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.setLocalPort(puertoPeticion);
        return new ManagementPortRequestMatcher(env).matches(request);
    }

    @Test
    void coincide_solo_con_la_peticion_que_entra_por_el_puerto_de_gestion() {
        assertTrue(coincide(8090, 8080, 8090));
        assertFalse(coincide(8090, 8080, 8080));
    }

    @Test
    void sin_servidor_de_gestion_nunca_coincide() {
        assertFalse(coincide(null, 8080, 8080));
        assertFalse(coincide(0, 8080, 0));
        assertFalse(coincide(-1, 8080, 8080));
    }

    @Test
    void si_gestion_y_api_comparten_puerto_nunca_coincide() {
        assertFalse(coincide(8080, 8080, 8080));
    }

    @Test
    void exige_el_environment() {
        assertThrows(NullPointerException.class, () -> new ManagementPortRequestMatcher(null));
    }
}
