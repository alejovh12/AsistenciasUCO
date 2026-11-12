package co.edu.uco.asistenciasuco.infrastructure.config.management;

import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPublicEndpoints;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 D.9-a (CONTRACT D §11.1, F-TRX-020): Actuator sale por un puerto de gestion propio (8090, loopback) y el puerto de la API
 * solo deja publico {@code /actuator/health/**}; {@code /actuator/prometheus} ya no es publico ni esta exento del PEP.
 */
class ManagementPortConfigTest {

    @Test
    void application_yml_separa_el_puerto_de_gestion_en_loopback_con_defecto_8090() {
        final Properties propiedades = new Properties();
        try {
            for (final org.springframework.core.env.PropertySource<?> fuente
                    : new org.springframework.boot.env.YamlPropertySourceLoader().load("application", new org.springframework.core.io.FileSystemResource("src/main/resources/application.yml"))) {
                final org.springframework.core.env.EnumerablePropertySource<?> enumerable = (org.springframework.core.env.EnumerablePropertySource<?>) fuente;
                for (final String nombre : enumerable.getPropertyNames()) {
                    propiedades.putIfAbsent(nombre, String.valueOf(enumerable.getProperty(nombre)));
                }
            }
        } catch (final java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }

        assertEquals("${MANAGEMENT_SERVER_PORT:8090}", propiedades.getProperty("management.server.port"));
        assertEquals("${MANAGEMENT_SERVER_ADDRESS:127.0.0.1}", propiedades.getProperty("management.server.address"));
        assertTrue(propiedades.getProperty("management.endpoints.web.exposure.include").contains("prometheus"));
    }

    @Test
    void solo_health_es_publico_en_el_puerto_de_la_api() {
        assertEquals("/actuator/health/**", SecurityPublicEndpoints.ACTUATOR);
        assertTrue(exento("/actuator/health"));
        assertTrue(exento("/actuator/health/liveness"));
        assertFalse(exento("/actuator/prometheus"));
        assertFalse(exento("/actuator/env"));
        assertFalse(exento("/actuator"));
    }

    @Test
    void el_puerto_de_gestion_no_puede_coincidir_con_el_de_la_api() {
        assertDoesNotThrow(() -> new ManagementServerPortGuard(8080, 8090));
        assertDoesNotThrow(() -> new ManagementServerPortGuard(8080, -1));
        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> new ManagementServerPortGuard(8080, 8080));
        assertTrue(error.getMessage().contains("MANAGEMENT_SERVER_PORT"));
    }

    private static boolean exento(final String ruta) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", ruta);
        request.setRequestURI(ruta);
        return SecurityPublicEndpoints.pepExempt().matches(request);
    }
}
