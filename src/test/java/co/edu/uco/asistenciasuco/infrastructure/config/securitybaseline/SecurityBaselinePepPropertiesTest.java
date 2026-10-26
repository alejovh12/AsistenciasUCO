package co.edu.uco.asistenciasuco.infrastructure.config.securitybaseline;

import co.edu.uco.asistenciasuco.infrastructure.config.adapters.securitybaseline.SecurityBaselinePepConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePepProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityPepEnforcementFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBaselinePepPropertiesTest {

    private static final String CREDENTIAL = "credential-value-never-logged";

    @Test
    void http_solo_hacia_loopback_y_https_en_cualquier_otro_host() {
        for (final String url : List.of("http://127.0.0.1:18081", "http://localhost:18081",
                "http://[::1]:18081", "https://pep.example.org", "https://pep.example.org/")) {
            assertDoesNotThrow(() -> enabled(url, Duration.ofSeconds(2)), url);
        }
        for (final String url : List.of("http://pep.example.org", "http://10.0.0.5:18081",
                "http://keycloak.localhost:18081", "ftp://127.0.0.1")) {
            assertThrows(IllegalStateException.class, () -> enabled(url, Duration.ofSeconds(2)), url);
        }
    }

    @Test
    void url_del_pep_es_un_origen_fijo_sin_credenciales_query_fragment_ni_path() {
        for (final String url : List.of("https://user:pass@pep.example.org", "https://pep.example.org?next=x",
                "https://pep.example.org#x", "https://pep.example.org/otro-prefijo")) {
            assertThrows(IllegalStateException.class, () -> enabled(url, Duration.ofSeconds(2)), url);
        }
        assertEquals(URI.create("https://pep.example.org/internal/v1/embedded-access-decisions"),
                enabled("https://pep.example.org", Duration.ofSeconds(2)).embeddedDecisionEndpoint());
    }

    @Test
    void timeouts_positivos_y_acotados() {
        assertThrows(IllegalStateException.class, () -> enabled("https://pep.example.org", Duration.ZERO));
        assertThrows(IllegalStateException.class, () -> enabled("https://pep.example.org", Duration.ofSeconds(31)));
        assertDoesNotThrow(() -> enabled("https://pep.example.org", Duration.ofSeconds(30)));
    }

    @Test
    void nombre_y_ambiente_deben_ser_identificadores_validos() {
        assertThrows(IllegalStateException.class, () -> new SecurityBaselinePepProperties(
                URI.create("https://pep.example.org"), "nombre con espacios", "dev", CREDENTIAL, null, null));
        assertThrows(IllegalStateException.class, () -> new SecurityBaselinePepProperties(
                URI.create("https://pep.example.org"), "asistencias-uco", " ", CREDENTIAL, null, null));
        assertThrows(IllegalStateException.class, () -> new SecurityBaselinePepProperties(
                URI.create("https://pep.example.org"), "asistencias-uco", "dev", " ", null, null));
    }

    @Test
    void ambiente_debe_ser_dev_lab_o_prod_como_exige_el_pdp_017() {
        for (final String environment : new String[] {"dev", "lab", "prod"}) {
            assertDoesNotThrow(() -> new SecurityBaselinePepProperties(
                    URI.create("https://pep.example.org"), "asistencias-uco", environment, CREDENTIAL, null, null));
        }
        for (final String environment : new String[] {"local", "DEV", "staging"}) {
            assertThrows(IllegalStateException.class, () -> new SecurityBaselinePepProperties(
                    URI.create("https://pep.example.org"), "asistencias-uco", environment, CREDENTIAL, null, null));
        }
    }

    @Test
    void to_string_nunca_expone_la_credencial() {
        final String rendered = enabled("https://pep.example.org", Duration.ofSeconds(2)).toString();
        assertFalse(rendered.contains(CREDENTIAL));
        assertTrue(rendered.contains("<redacted>"));
    }

    @Test
    void configuracion_incompleta_no_exige_una_bandera_para_ser_bloqueada() {
        assertThrows(IllegalStateException.class, () -> new SecurityBaselinePepProperties(
                null, null, null, null, null, null));
    }

    @Test
    void legacy_no_crea_cliente_pep_baseline() {
        contextRunner()
                .withPropertyValues("app.adapters.security.provider=legacy")
                .run(context -> {
                    assertTrue(context.getBeansOfType(SecurityPepEnforcementFilter.class).isEmpty());
                    assertTrue(context.getBeansOfType(FilterRegistrationBean.class).isEmpty());
                });
    }

    @Test
    void baseline_crea_unico_filtro_con_registro_servlet_deshabilitado() {
        contextRunner()
                .withBean(co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter.class)
                .withPropertyValues(
                        "app.adapters.security.provider=baseline",
                        "app.providers.security-baseline-pep.pep-url=http://127.0.0.1:18081",
                        "app.providers.security-baseline-pep.application-name=asistencias-uco-lab",
                        "app.providers.security-baseline-pep.environment=dev",
                        "app.providers.security-baseline-pep.application-credential=" + CREDENTIAL)
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(SecurityPepEnforcementFilter.class).size());
                    final FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
                    assertFalse(registration.isEnabled());
                });
    }

    @Test
    void baseline_con_url_insegura_falla_el_arranque() {
        contextRunner()
                .withBean(co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter.class)
                .withPropertyValues(
                        "app.adapters.security.provider=baseline",
                        "app.providers.security-baseline-pep.pep-url=http://pep.example.org",
                        "app.providers.security-baseline-pep.application-name=asistencias-uco-lab",
                        "app.providers.security-baseline-pep.environment=dev",
                        "app.providers.security-baseline-pep.application-credential=" + CREDENTIAL)
                .run(context -> assertTrue(context.getStartupFailure() != null));
    }

    private static ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(SecurityBaselinePepConfiguration.class);
    }

    private static SecurityBaselinePepProperties enabled(final String url, final Duration timeout) {
        return new SecurityBaselinePepProperties(URI.create(url), "asistencias-uco-lab", "dev",
                CREDENTIAL, timeout, timeout);
    }
}
