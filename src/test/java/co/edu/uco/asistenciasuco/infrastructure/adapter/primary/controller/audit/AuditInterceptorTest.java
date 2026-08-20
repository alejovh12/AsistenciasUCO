package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.audit;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditActorType;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditOutcome;
import co.edu.uco.asistenciasuco.infrastructure.audit.AuditRequestAttributes;
import co.edu.uco.asistenciasuco.infrastructure.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.tracing.TraceContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditInterceptorTest {

    private static final UUID CORRELATION_ID = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    private final CapturingAuditEventPublisher publisher = new CapturingAuditEventPublisher();
    private final AuditInterceptor interceptor = new AuditInterceptor(
            publisher,
            new ClientIpResolver(),
            new RequestActorResolver()
    );

    @AfterEach
    void clearCorrelation() {
        CorrelationIdContext.clear();
        MDC.clear();
    }

    @Test
    void afterCompletion_publica_evento_auditable_exitoso_con_actor_y_correlationId() throws Exception {
        CorrelationIdContext.set(CORRELATION_ID);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/grupos/" + CORRELATION_ID + "/estudiantes");
        request.setQueryString("password=secreto&pagina=1");
        request.setRemoteAddr("10.0.0.7");
        request.setUserPrincipal(jwtAuthentication("usuario-123"));
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("grupoId", CORRELATION_ID.toString()));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        MDC.put(TraceContextSnapshot.TRACE_ID_MDC_KEY, "43150ef63935f555cbe9832b808a84a7");
        MDC.put(TraceContextSnapshot.SPAN_ID_MDC_KEY, "7cbda8082fa53066");

        interceptor.afterCompletion(request, response, handlerMethod("registrarEstudiante"), null);

        final AuditEvent event = publisher.event.get();
        assertEquals("usuario-123", event.actorId());
        assertEquals(AuditActorType.USER, event.actorType());
        assertEquals("REGISTRAR_ESTUDIANTE_EN_GRUPO", event.action());
        assertEquals("GRUPO", event.resourceType());
        assertEquals(CORRELATION_ID.toString(), event.resourceId());
        assertEquals(CORRELATION_ID.toString(), event.correlationId());
        assertEquals("43150ef63935f555cbe9832b808a84a7", event.traceId());
        assertEquals("7cbda8082fa53066", event.spanId());
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
        assertEquals("10.0.0.7", event.clientIp());
        assertEquals(201, event.httpStatus());
        assertEquals(Map.of("handlerType", "HTTP"), event.metadata());
    }

    @Test
    void afterCompletion_distingue_resultado_fallido() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/usuarios");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(409);
        AuditRequestAttributes.storeErrorCode(request, "ERR_UNICIDAD_CORREO");

        interceptor.afterCompletion(request, response, handlerMethod("crearUsuario"), new RuntimeException("fallo"));

        assertEquals(AuditOutcome.FAILURE, publisher.event.get().outcome());
        assertEquals("ERR_UNICIDAD_CORREO", publisher.event.get().errorCode());
    }

    @Test
    void afterCompletion_resuelve_resourceId_desde_requestBody_y_actor_anonimo() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/sesiones/cierres");
        AuditRequestAttributes.storeRequestBody(request, new CerrarSesionRequest(CORRELATION_ID));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        interceptor.afterCompletion(request, response, handlerMethod("cerrarSesion"), null);

        final AuditEvent event = publisher.event.get();
        assertEquals(AuditActorType.ANONYMOUS, event.actorType());
        assertNull(event.actorId());
        assertEquals(CORRELATION_ID.toString(), event.resourceId());
    }

    @Test
    void afterCompletion_resuelve_resourceId_desde_responseBody() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/usuarios");
        AuditRequestAttributes.storeResponseBody(request, new CrearUsuarioResponse(CORRELATION_ID));
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        interceptor.afterCompletion(request, response, handlerMethod("crearUsuario"), null);

        assertEquals(CORRELATION_ID.toString(), publisher.event.get().resourceId());
    }

    @Test
    void afterCompletion_ignora_metodos_no_anotados() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/grupos");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, handlerMethod("consultar"), null);

        assertNull(publisher.event.get());
    }

    private HandlerMethod handlerMethod(final String methodName) throws NoSuchMethodException {
        final Controller controller = new Controller();
        final Method method = Controller.class.getDeclaredMethod(methodName);
        return new HandlerMethod(controller, method);
    }

    private JwtAuthenticationToken jwtAuthentication(final String idUsuario) {
        final Instant now = Instant.now();
        final Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                .subject("keycloak-sub-123")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(List.of("asistencias-api"))
                .claim("idUsuario", idUsuario)
                .build();
        return new JwtAuthenticationToken(jwt, List.of(), idUsuario);
    }

    private static final class CapturingAuditEventPublisher implements AuditEventPublisher {

        private final AtomicReference<AuditEvent> event = new AtomicReference<>();

        @Override
        public void publish(final AuditEvent event) {
            this.event.set(event);
        }
    }

    private static final class Controller {

        @AuditableOperation(
                action = "REGISTRAR_ESTUDIANTE_EN_GRUPO",
                resourceType = "GRUPO",
                resourceIdPathVariable = "grupoId"
        )
        void registrarEstudiante() {
        }

        @AuditableOperation(
                action = "CREAR_USUARIO",
                resourceType = "USUARIO",
                resourceIdResponseField = "usuarioId"
        )
        void crearUsuario() {
        }

        @AuditableOperation(action = "CERRAR_SESION", resourceType = "SESION", resourceIdRequestField = "sesion")
        void cerrarSesion() {
        }

        void consultar() {
        }
    }

    private static final class CerrarSesionRequest {

        private final UUID sesion;

        private CerrarSesionRequest(final UUID sesion) {
            this.sesion = sesion;
        }

        public UUID getSesion() {
            return sesion;
        }
    }

    private static final class CrearUsuarioResponse {

        private final UUID usuarioId;

        private CrearUsuarioResponse(final UUID usuarioId) {
            this.usuarioId = usuarioId;
        }

        public UUID getUsuarioId() {
            return usuarioId;
        }
    }
}
