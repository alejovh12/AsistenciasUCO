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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SEC-AZ-001: sin la propiedad {@code app.security.azure-events.webhook-token} el webhook debe
 * rechazar (fail-closed) y no existe ningún valor por defecto funcional, incluido el default
 * histórico retirado por TD-051 (se conserva aquí solo como literal de regresión).
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
        AzureWebhookUnconfiguredSecurityChainTest.SupportConfig.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class AzureWebhookUnconfiguredSecurityChainTest {

    private static final String WEBHOOK_URI = "/api/v1/internal/azure-events";
    private static final String LEGACY_REMOVED_DEFAULT = "dev-azure-event-token-change-in-prod";
    private static final String EVENT = "[{\"eventType\":\"Microsoft.KeyVault.SecretNewVersionCreated\",\"subject\":\"x\",\"data\":{}}]";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcesarEventoAzureInputPort procesarEventoAzureInputPort;

    @Test
    void sin_credencial_configurada_el_default_historico_ya_no_autentica() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", LEGACY_REMOVED_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(procesarEventoAzureInputPort);
    }

    @Test
    void sin_credencial_configurada_ningun_header_ni_query_autentica() throws Exception {
        mockMvc.perform(post(WEBHOOK_URI).contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(WEBHOOK_URI + "?token=" + LEGACY_REMOVED_DEFAULT)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(WEBHOOK_URI).header("aeg-sas-token", "")
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isUnauthorized());
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
