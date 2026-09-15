package co.edu.uco.asistenciasuco.infrastructure.config.adapters.security.keycloak;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.InstitutionalJwtAuthenticationConverter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.KeycloakSecurityProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica el Composition Root de runtime security SIN conectar a un Keycloak real: el
 * {@code JwtDecoder} productivo se construye con {@code JwtDecoders.fromIssuerLocation(...)},
 * que hace una llamada HTTP al issuer al crearse.
 *
 * <p>{@code KeycloakSecurityAdapterConfiguration.jwtDecoder(...)} es, a propósito, el único
 * dueño de esa política cuando el provider es Keycloak — ya NO lleva
 * {@code @ConditionalOnMissingBean}, para que nada pueda sustituirlo silenciosamente en
 * producción. Por eso este test, para evitar la llamada HTTP real, sustituye ese bean
 * explícitamente habilitando {@code allow-bean-definition-overriding} solo en su propio
 * contexto — nunca en la configuración productiva.</p>
 */
class KeycloakSecurityAdapterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    KeycloakSecurityAdapterConfiguration.class,
                    FakeJwtDecoderConfiguration.class
            )
            .withAllowBeanDefinitionOverriding(true)
            .withPropertyValues(
                    "app.adapters.security.provider=keycloak",
                    "app.providers.keycloak-security.issuer-uri=http://localhost:8081/realms/asistencias-uco",
                    "app.providers.keycloak-security.api-client-id=asistencias-api",
                    "app.providers.keycloak-security.expected-audience=asistencias-api",
                    "app.providers.keycloak-security.user-id-claim=idUsuario"
            );

    @Test
    void registra_exactamente_un_jwtClaimsAdapter_keycloak() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(JwtClaimsExtractor.class).size());
            assertInstanceOf(KeycloakJwtClaimsExtractor.class, context.getBean(JwtClaimsExtractor.class));
        });
    }

    @Test
    void registra_properties_tipadas_con_valores_configurados() {
        contextRunner.run(context -> {
            final KeycloakSecurityProviderProperties properties =
                    context.getBean(KeycloakSecurityProviderProperties.class);
            assertEquals("asistencias-api", properties.apiClientId());
            assertEquals("idUsuario", properties.userIdClaim());
        });
    }

    @Test
    void institutional_jwt_authentication_converter_no_se_registra_en_este_composition_root() {
        // InstitutionalJwtAuthenticationConverter es neutral y se registra en SecurityConfig,
        // no en el Composition Root de Keycloak: aquí solo vive lo que SÍ conoce Keycloak.
        contextRunner.run(context ->
                assertFalse(context.containsBean("institutionalJwtAuthenticationConverter"))
        );
        contextRunner.run(context ->
                assertTrue(context.getBeansOfType(InstitutionalJwtAuthenticationConverter.class).isEmpty())
        );
    }

    @Test
    void provider_distinto_de_keycloak_no_registra_nada_de_este_composition_root() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        KeycloakSecurityAdapterConfiguration.class,
                        FakeJwtDecoderConfiguration.class
                )
                .withAllowBeanDefinitionOverriding(true)
                .withPropertyValues(
                        "app.adapters.security.provider=otro-provider-hipotetico"
                )
                .run(context -> assertTrue(context.getBeansOfType(JwtClaimsExtractor.class).isEmpty()));
    }

    @Test
    void jwtDecoder_productivo_no_lleva_conditional_on_missing_bean() throws NoSuchMethodException {
        final boolean hasConditionalOnMissingBean = KeycloakSecurityAdapterConfiguration.class
                .getMethod("jwtDecoder", KeycloakSecurityProviderProperties.class)
                .isAnnotationPresent(org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean.class);

        assertFalse(hasConditionalOnMissingBean);
    }

    @Configuration(proxyBeanMethods = false)
    static class FakeJwtDecoderConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("No se conecta a un Keycloak real en este test.");
            };
        }
    }
}
