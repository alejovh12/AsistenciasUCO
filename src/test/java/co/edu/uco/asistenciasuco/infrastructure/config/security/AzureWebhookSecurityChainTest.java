package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.application.features.admin.procesareventoazure.primaryports.ProcesarEventoAzureInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.azure.AzureEventGridWebhookController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-AZ-001..007 sobre la cadena real de {@link SecurityConfig} con el controller real del
 * webhook (InputPort simulado): credencial configurada con un valor TEST_ONLY, header único
 * {@code aeg-sas-token}, {@code permitAll} de autorización != endpoint sin autenticación.
 */
@WebMvcTest(controllers = AzureEventGridWebhookController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        AzureWebhookSecurityChainTest.SupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "app.security.azure-events.webhook-token=unit-test-webhook-token"
})
class AzureWebhookSecurityChainTest {

    private static final String WEBHOOK_URI = "/api/v1/internal/azure-events";
    private static final String TEST_ONLY_WEBHOOK_TOKEN = "unit-test-webhook-token";
    private static final String REGULAR_EVENT = """
            [{"eventType":"Microsoft.AppConfiguration.KeyValueModified",
              "subject":"asistencias:max_inasistencias",
              "data":{"key":"asistencias:max_inasistencias"}}]
            """;
    private static final String VALIDATION_EVENT = """
            [{"eventType":"Microsoft.EventGrid.SubscriptionValidationEvent",
              "data":{"validationCode":"echo-test-only-123"}}]
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcesarEventoAzureInputPort procesarEventoAzureInputPort;

    @Test
    void query_only_con_la_credencial_correcta_y_sin_header_responde_401() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI + "?token=" + TEST_ONLY_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(WEBHOOK_URI + "?access_token=" + TEST_ONLY_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(WEBHOOK_URI + "?key=" + TEST_ONLY_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(procesarEventoAzureInputPort);
    }

    @Test
    void header_ausente_responde_401_y_no_es_403_de_csrf() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(procesarEventoAzureInputPort);
    }

    @Test
    void header_vacio_o_incorrecto_responde_401() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", "")
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", "wrong-test-only-token")
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(procesarEventoAzureInputPort);
    }

    @Test
    void handshake_de_validacion_se_preserva_con_header_valido() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN)
                        .header("aeg-event-type", "SubscriptionValidation")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDATION_EVENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationResponse").value("echo-test-only-123"));
        verify(procesarEventoAzureInputPort, never()).procesarEvento(anyString(), anyString(), anyMap());
    }

    @Test
    void handshake_sin_credencial_no_se_responde() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI)
                        .header("aeg-event-type", "SubscriptionValidation")
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDATION_EVENT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void evento_regular_se_procesa_con_header_valido_sin_csrf() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN)
                        .header("aeg-event-type", "Notification")
                        .contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.processed").value(1));
        verify(procesarEventoAzureInputPort).procesarEvento(
                eq("Microsoft.AppConfiguration.KeyValueModified"),
                eq("asistencias:max_inasistencias"),
                eq(Map.of("key", "asistencias:max_inasistencias")));
    }

    @Test
    void la_api_de_negocio_sigue_protegida_y_el_header_del_webhook_no_la_abre() throws Exception {
        // AS-IS inalterado: GET sin JWT => 401; POST sin Bearer ni CSRF => 403 (CSRF sigue activo
        // fuera del webhook). El header del webhook no abre ninguna ruta de negocio.
        mockMvc.perform(get("/api/v1/sesiones"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/sesiones").header("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/sesiones").header("aeg-sas-token", TEST_ONLY_WEBHOOK_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verify(procesarEventoAzureInputPort, never()).procesarEvento(any(), any(), any());
    }

    @Test
    void ruta_que_solo_termina_igual_no_queda_exenta_del_webhook_ni_publica() throws Exception {
        mockMvc.perform(post("/otro" + WEBHOOK_URI).contentType(MediaType.APPLICATION_JSON).content(REGULAR_EVENT))
                .andExpect(status().is4xxClientError());
        verifyNoInteractions(procesarEventoAzureInputPort);
    }

    @TestConfiguration
    static class SupportConfig {

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor("asistencias-api", "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("Invalid test token.");
            };
        }

        @Bean
        co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> java.util.Optional.empty();
        }

        @Bean
        co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
