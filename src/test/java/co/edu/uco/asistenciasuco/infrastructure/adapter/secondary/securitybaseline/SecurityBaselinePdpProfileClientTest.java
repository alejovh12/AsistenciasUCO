package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.securitybaseline;

import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Estado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Resultado;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SecurityBaselinePdpAdminProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PDP simulado con el contrato 0.1.7 que ya consumen los scripts del proyecto (listados paginados, asignaciones). */
class SecurityBaselinePdpProfileClientTest {

    private static final String APP_ID = "app-1";
    private static final String PERFIL_DOCENCIA = "Docencia";
    private static final Instant AHORA = Instant.parse("2026-10-26T20:00:00Z");

    private HttpServer server;
    private final List<String> llamadas = new CopyOnWriteArrayList<>();
    private volatile int tokenStatus = 200;
    private volatile int pdpStatusOverride = 0;
    private volatile int assignStatus = 201;
    private volatile String usersJson = "[{\"id\":\"u-1\",\"email\":\"Persona@uco.edu.co\"}]";
    private volatile String profileAssignmentsJson = "{\"content\":[],\"total\":0}";
    private volatile String roleAssignmentsJson = "{\"content\":[],\"total\":0}";
    private volatile int deleteStatus = 204;

    @BeforeEach
    void arrancarPdp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/token", exchange -> {
            llamadas.add("POST /token");
            if (tokenStatus != 200) {
                responder(exchange, tokenStatus, "{}");
                return;
            }
            responder(exchange, 200, "{\"access_token\":\"tok\",\"expires_in\":300}");
        });
        server.createContext("/api/v1/", exchange -> {
            llamadas.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            if (!"Bearer tok".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                responder(exchange, 401, "{}");
                return;
            }
            if (pdpStatusOverride != 0) {
                responder(exchange, pdpStatusOverride, "{}");
                return;
            }
            final String path = exchange.getRequestURI().getPath();
            final String method = exchange.getRequestMethod();
            if (path.equals("/api/v1/applications")) {
                responder(exchange, 200, "{\"data\":[{\"id\":\"otra\",\"name\":\"otra-app\"},{\"id\":\"" + APP_ID + "\",\"name\":\"asistencias-uco\"}]}");
            } else if (path.equals("/api/v1/applications/" + APP_ID + "/security/profiles")) {
                responder(exchange, 200, "{\"data\":{\"content\":[{\"id\":\"p-doc\",\"name\":\"Docencia\"},{\"id\":\"p-est\",\"name\":\"Estudiantes\"}],\"total\":2}}");
            } else if (path.equals("/api/v1/applications/" + APP_ID + "/security/roles")) {
                responder(exchange, 200, "{\"data\":{\"content\":[{\"id\":\"r-doc\",\"name\":\"DOCENTE\"}],\"total\":1}}");
            } else if (path.equals("/api/v1/users")) {
                responder(exchange, 200, "{\"data\":" + (usersJson.startsWith("[") ? usersJson : usersJson) + "}");
            } else if (path.equals("/api/v1/profiles/p-doc/assignments") && method.equals("GET")) {
                responder(exchange, 200, "{\"data\":" + profileAssignmentsJson + "}");
            } else if (path.equals("/api/v1/profiles/p-doc/assignments") && method.equals("POST")) {
                responder(exchange, assignStatus, "{\"code\":\"X\"}");
            } else if (path.startsWith("/api/v1/profiles/p-doc/assignments/") && method.equals("DELETE")) {
                responder(exchange, deleteStatus, "");
            } else if (path.equals("/api/v1/roles/r-doc/assignments")) {
                responder(exchange, 200, "{\"data\":" + roleAssignmentsJson + "}");
            } else {
                responder(exchange, 404, "{}");
            }
        });
        server.start();
    }

    @AfterEach
    void detener() {
        server.stop(0);
    }

    private static void responder(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private SecurityBaselinePdpProfileClient client(final String secret) {
        final String base = "http://127.0.0.1:" + server.getAddress().getPort();
        final SecurityBaselinePdpAdminProperties properties = new SecurityBaselinePdpAdminProperties(
                URI.create(base), URI.create(base + "/token"), "asistencias-provisioner", secret, true, "asistencias-uco",
                Map.of(InstitutionalRole.ADMINISTRADOR, "Administración institucional", InstitutionalRole.DECANO, "Decanatura",
                        InstitutionalRole.COORDINADOR, "Coordinación de programa", InstitutionalRole.DOCENTE, PERFIL_DOCENCIA,
                        InstitutionalRole.ESTUDIANTE, "Estudiantes"),
                Duration.ofSeconds(2), Duration.ofSeconds(5));
        return new SecurityBaselinePdpProfileClient(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(),
                JsonMapper.builder().build(), properties, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static String asignacion(final String id, final String userId, final String validUntil) {
        return "{\"id\":\"" + id + "\",\"userId\":\"" + userId + "\",\"validUntil\":" + (validUntil == null ? "null" : "\"" + validUntil + "\"") + "}";
    }

    @Test
    void sincronizar_asigna_el_perfil_cuando_la_persona_existe_y_no_lo_tiene() {
        final Resultado resultado = client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.ASIGNADO, resultado.estado());
        assertTrue(llamadas.contains("POST /api/v1/profiles/p-doc/assignments"));
    }

    @Test
    void sincronizar_es_idempotente_si_ya_tiene_una_asignacion_vigente() {
        profileAssignmentsJson = "{\"content\":[" + asignacion("a-1", "u-1", null) + "],\"total\":1}";

        final Resultado resultado = client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.ASIGNADO, resultado.estado());
        assertFalse(llamadas.contains("POST /api/v1/profiles/p-doc/assignments"));
    }

    @Test
    void una_asignacion_vencida_no_cuenta_como_vigente_y_se_reasigna() {
        profileAssignmentsJson = "{\"content\":[" + asignacion("a-0", "u-1", "2026-10-01T00:00:00Z") + "],\"total\":1}";

        assertEquals(Estado.ASIGNADO, client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
        assertTrue(llamadas.contains("POST /api/v1/profiles/p-doc/assignments"));
    }

    @Test
    void persona_que_aun_no_existe_en_el_pdp_queda_pendiente_sin_asignar() {
        usersJson = "[]";

        final Resultado resultado = client("s").sincronizar("nueva@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.PENDIENTE_PERSONA_NO_REGISTRADA, resultado.estado());
        assertFalse(llamadas.contains("POST /api/v1/profiles/p-doc/assignments"));
    }

    @Test
    void conflicto_409_con_rol_directo_vigente_se_informa_como_acceso_efectivo() {
        assignStatus = 409;
        roleAssignmentsJson = "{\"content\":[" + asignacion("d-1", "u-1", null) + "],\"total\":1}";

        assertEquals(Estado.ASIGNADO, client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void conflicto_409_sin_rol_directo_es_rechazado() {
        assignStatus = 409;

        assertEquals(Estado.RECHAZADO, client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void perfil_inexistente_en_el_pdp_es_rechazado_sin_asignar() {
        final Resultado resultado = client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.COORDINADOR);

        assertEquals(Estado.RECHAZADO, resultado.estado());
        assertTrue(resultado.detalle().contains("Coordinación de programa"));
    }

    @Test
    void revocar_cierra_solo_las_asignaciones_vigentes_de_esa_persona() {
        profileAssignmentsJson = "{\"content\":[" + asignacion("a-1", "u-1", null) + "," + asignacion("a-2", "u-9", null)
                + "," + asignacion("a-3", "u-1", "2026-09-01T00:00:00Z") + "],\"total\":3}";

        final Resultado resultado = client("s").revocar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.NO_ASIGNADO, resultado.estado());
        assertTrue(llamadas.contains("DELETE /api/v1/profiles/p-doc/assignments/a-1"));
        assertFalse(llamadas.contains("DELETE /api/v1/profiles/p-doc/assignments/a-2"));
        assertFalse(llamadas.contains("DELETE /api/v1/profiles/p-doc/assignments/a-3"));
    }

    @Test
    void revocar_con_rol_directo_residual_pide_revision_manual() {
        roleAssignmentsJson = "{\"content\":[" + asignacion("d-1", "u-1", null) + "],\"total\":1}";

        assertEquals(Estado.RECHAZADO, client("s").revocar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void revocar_a_una_persona_desconocida_para_el_pdp_no_tiene_nada_que_revocar() {
        usersJson = "[]";

        assertEquals(Estado.NO_ASIGNADO, client("s").revocar("nueva@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void consultar_no_modifica_nada() {
        final Resultado resultado = client("s").consultar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.NO_ASIGNADO, resultado.estado());
        assertTrue(llamadas.stream().noneMatch(l -> l.startsWith("POST /api/v1") || l.startsWith("DELETE")));
    }

    @Test
    void sin_secreto_configurado_la_sincronizacion_queda_pendiente_sin_llamar_al_pdp() {
        final Resultado resultado = client("").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.PENDIENTE_NO_DISPONIBLE, resultado.estado());
        assertTrue(llamadas.isEmpty());
    }

    @Test
    void el_pdp_que_rechaza_la_credencial_de_servicio_deja_la_sincronizacion_pendiente() {
        pdpStatusOverride = 401;

        final Resultado resultado = client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(Estado.PENDIENTE_NO_DISPONIBLE, resultado.estado());
        assertTrue(resultado.detalle().contains("401"));
    }

    @Test
    void error_5xx_del_pdp_deja_la_sincronizacion_pendiente() {
        pdpStatusOverride = 503;

        assertEquals(Estado.PENDIENTE_NO_DISPONIBLE,
                client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void sin_token_de_servicio_la_sincronizacion_queda_pendiente() {
        tokenStatus = 401;

        assertEquals(Estado.PENDIENTE_NO_DISPONIBLE,
                client("s").sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void pdp_caido_deja_la_sincronizacion_pendiente_sin_lanzar() {
        final SecurityBaselinePdpProfileClient cliente = client("s");
        server.stop(0);

        assertEquals(Estado.PENDIENTE_NO_DISPONIBLE, cliente.sincronizar("persona@uco.edu.co", InstitutionalRole.DOCENTE).estado());
    }

    @Test
    void el_token_de_servicio_se_reutiliza_entre_operaciones() {
        final SecurityBaselinePdpProfileClient cliente = client("s");

        cliente.consultar("persona@uco.edu.co", InstitutionalRole.DOCENTE);
        cliente.consultar("persona@uco.edu.co", InstitutionalRole.DOCENTE);

        assertEquals(1, Collections.frequency(new ArrayList<>(llamadas), "POST /token"));
    }

    @Test
    void entradas_invalidas_son_rechazadas_sin_llamar_al_pdp() {
        assertEquals(Estado.RECHAZADO, client("s").sincronizar(" ", InstitutionalRole.DOCENTE).estado());
        assertEquals(Estado.RECHAZADO, client("s").sincronizar("persona@uco.edu.co", null).estado());
        assertTrue(llamadas.isEmpty());
    }
}
