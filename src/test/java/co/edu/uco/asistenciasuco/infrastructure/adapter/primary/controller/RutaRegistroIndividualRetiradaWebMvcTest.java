package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.SecurityContextAuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 D.7 (CONTRACT D 8.3, TD-009, HU027/HU033): {@code POST /api/v1/asistencias} (registro individual) queda sin
 * handler y responde 404 con {@code ApiErrorResponse}, aun con el token del docente que antes tenia acceso. El lote
 * {@code POST /api/v1/asistencias/lote} sigue atendido. Usa el controlador real y simula sus dependencias por tipo de
 * constructor (sin nombrar tipos que D.7 borra).
 */
@WebMvcTest(useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "co\\.edu\\.uco\\.asistenciasuco\\.infrastructure\\.adapter\\.primary\\.controller\\..*\\.AsistenciaController"))
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        SecurityContextAuthenticatedUserResolver.class,
        RutaRegistroIndividualRetiradaWebMvcTest.SoporteSeguridad.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class RutaRegistroIndividualRetiradaWebMvcTest {

    private static final String AUDIENCE = "asistencias-api";
    private static final String ID = "11111111-1111-1111-1111-111111111111";
    private static final List<String> ROLES = List.of("COORDINADOR", "ADMINISTRADOR", "DOCENTE", "ESTUDIANTE", "DECANO");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void post_asistencias_responde_404_con_api_error_response_para_el_docente() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/asistencias")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estudiante\":\"" + ID + "\",\"grupo\":\"" + ID + "\",\"sesion\":\"" + ID + "\",\"presente\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/asistencias"));
    }

    @Test
    void el_lote_sigue_atendido_por_el_controlador() throws Exception {
        // Asercion positiva: el 403 de ESTUDIANTE prueba que la ruta del lote conserva su matcher (solo DOCENTE).
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/asistencias/lote")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ESTUDIANTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Registra un mock (RETURNS_DEFAULTS) por cada parametro de constructor de los {@code @RestController} incluidos
     * que no tenga bean. Asi la prueba no nombra tipos de produccion que A.1 borra.
     */
    static final class MocksParaControladoresIncluidosPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
            for (final String nombre : beanFactory.getBeanDefinitionNames()) {
                final BeanDefinition definicion = beanFactory.getBeanDefinition(nombre);
                final Optional<Class<?>> clase = cargar(definicion.getBeanClassName(), beanFactory.getBeanClassLoader());
                if (clase.isEmpty() || !clase.get().isAnnotationPresent(RestController.class)) {
                    continue;
                }
                for (final Constructor<?> constructor : clase.get().getDeclaredConstructors()) {
                    for (final Class<?> tipo : constructor.getParameterTypes()) {
                        if (beanFactory.getBeanNamesForType(tipo, true, false).length == 0) {
                            beanFactory.registerSingleton("mock#" + tipo.getName(), Mockito.mock(tipo));
                        }
                    }
                }
            }
        }

        private static Optional<Class<?>> cargar(final String nombreClase, final ClassLoader classLoader) {
            if (nombreClase == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(ClassUtils.forName(nombreClase, classLoader));
            } catch (ClassNotFoundException | LinkageError error) {
                return Optional.empty();
            }
        }
    }

    @TestConfiguration
    static class SoporteSeguridad {

        @Bean
        static BeanFactoryPostProcessor mocksParaControladoresIncluidosPostProcessor() {
            return new MocksParaControladoresIncluidosPostProcessor();
        }

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(AUDIENCE, "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!ROLES.contains(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", "22222222-2222-2222-2222-222222222222")
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", List.of(token))))
                        .build();
            };
        }

        @Bean
        ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> Optional.empty();
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
