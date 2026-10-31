package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.reporte;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
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
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.ReporteWiringConfiguration;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FIX-002 (SECURITY_FINDING DL-03 de SEC-BASELINE-003): el reporte Excel de asistencia de un
 * grupo debe aplicar la misma titularidad que los sub-recursos hermanos {@code /estudiantes} y
 * {@code /asistencias}. Cadena real: {@link SecurityConfig} (RBAC), resolver del usuario
 * autenticado, controller, interactor, caso de uso y {@link GlobalExceptionHandler}. Solo
 * {@link InstitutionalScopePort} y {@link ReporteAsistenciaQueryPort} se sustituyen.
 */
@WebMvcTest(controllers = ReporteAsistenciaController.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        SecurityContextAuthenticatedUserResolver.class,
        GlobalExceptionHandler.class,
        ReporteWiringConfiguration.class,
        ReporteAsistenciaTitularidadHttpTest.TestSupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class ReporteAsistenciaTitularidadHttpTest {

    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PATH = "/api/v1/grupos/{grupoId}/reportes/asistencia-excel";

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE_TITULAR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCENTE_AJENO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COORDINADOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ESTUDIANTE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID DOCENTE_SIN_IDENTIDAD = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID DOCENTE_COORDINADOR = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final UUID DOCENTE_COORDINADOR_AJENO = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private static final UUID COORDINADOR_AJENO = UUID.fromString("99999999-9999-9999-9999-999999999999");

    /** Token de prueba -> usuario y roles institucionales presentados por el JWT. */
    private static final Map<String, TokenIdentity> TOKENS = Map.of(
            "docente-titular", new TokenIdentity(DOCENTE_TITULAR, List.of("DOCENTE")),
            "docente-ajeno", new TokenIdentity(DOCENTE_AJENO, List.of("DOCENTE")),
            "docente-sin-identidad", new TokenIdentity(DOCENTE_SIN_IDENTIDAD, List.of("DOCENTE")),
            "coordinador", new TokenIdentity(COORDINADOR, List.of("COORDINADOR")),
            "coordinador-ajeno", new TokenIdentity(COORDINADOR_AJENO, List.of("COORDINADOR")),
            "docente-coordinador", new TokenIdentity(DOCENTE_COORDINADOR, List.of("DOCENTE", "COORDINADOR")),
            "docente-coordinador-ajeno", new TokenIdentity(DOCENTE_COORDINADOR_AJENO, List.of("DOCENTE", "COORDINADOR")),
            "estudiante", new TokenIdentity(ESTUDIANTE, List.of("ESTUDIANTE"))
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstitutionalScopePort institutionalScopePort;

    @MockitoBean
    private ReporteAsistenciaQueryPort reporteAsistenciaQueryPort;

    @BeforeEach
    void setUp() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_TITULAR)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_AJENO)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_SIN_IDENTIDAD)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findRolesActivosByUsuario(COORDINADOR)).thenReturn(Set.of(InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.findRolesActivosByUsuario(COORDINADOR_AJENO)).thenReturn(Set.of(InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_COORDINADOR))
                .thenReturn(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_COORDINADOR_AJENO))
                .thenReturn(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_TITULAR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_AJENO)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_SIN_IDENTIDAD)).thenReturn(Optional.empty());
        when(institutionalScopePort.findDocenteIdByUsuario(COORDINADOR)).thenReturn(Optional.empty());
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_COORDINADOR)).thenReturn(Optional.empty());
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_COORDINADOR_AJENO)).thenReturn(Optional.empty());
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_TITULAR, GRUPO)).thenReturn(true);
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_AJENO, GRUPO)).thenReturn(false);
        when(institutionalScopePort.canCoordinadorAccessGrupo(COORDINADOR, GRUPO)).thenReturn(true);
        when(institutionalScopePort.canCoordinadorAccessGrupo(DOCENTE_COORDINADOR, GRUPO)).thenReturn(true);
        when(institutionalScopePort.canCoordinadorAccessGrupo(DOCENTE_COORDINADOR_AJENO, GRUPO)).thenReturn(false);
        when(reporteAsistenciaQueryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(new ReporteAsistenciaRow(
                "G01", "Grupo 01", 1, "Sesion 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Ana Perez", "ana@uco.edu.co", Boolean.TRUE, null)));
    }

    @Test
    void docente_ajeno_al_grupo_recibe_403_forbidden_json_sin_generar_reporte() throws Exception {
        mockMvc.perform(get(PATH, GRUPO)
                        .param("usuarioEjecutor", DOCENTE_TITULAR.toString())
                        .param("idUsuario", DOCENTE_TITULAR.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer("docente-ajeno")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/api/v1/grupos/" + GRUPO + "/reportes/asistencia-excel"))
                .andExpect(content().string(not(containsString("Ana Perez"))));

        verify(institutionalScopePort).findRolesActivosByUsuario(DOCENTE_AJENO);
        verify(institutionalScopePort).canDocenteAccessGrupo(DOCENTE_AJENO, GRUPO);
        verify(institutionalScopePort, never()).findRolesActivosByUsuario(DOCENTE_TITULAR);
        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void docente_titular_descarga_el_reporte_xlsx() throws Exception {
        final MvcResult result = mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, bearer("docente-titular")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("reporte-asistencia-" + GRUPO + ".xlsx")))
                .andReturn();

        assertEquals("Ana Perez", estudianteDeLaPrimeraFila(result));
    }

    @Test
    void coordinador_con_scope_del_grupo_conserva_acceso_institucional() throws Exception {
        final MvcResult result = mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, bearer("coordinador")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andReturn();

        assertEquals("Ana Perez", estudianteDeLaPrimeraFila(result));
        verify(institutionalScopePort).canCoordinadorAccessGrupo(COORDINADOR, GRUPO);
    }

    @Test
    void coordinador_fuera_de_su_scope_recibe_403_sin_consultar_el_reporte() throws Exception {
        mockMvc.perform(get(PATH, GRUPO).header(HttpHeaders.AUTHORIZATION, bearer("coordinador-ajeno")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(institutionalScopePort).canCoordinadorAccessGrupo(COORDINADOR_AJENO, GRUPO);
        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void docente_sin_identidad_institucional_resuelta_recibe_403_sin_bypass() throws Exception {
        mockMvc.perform(get(PATH, GRUPO).header(HttpHeaders.AUTHORIZATION, bearer("docente-sin-identidad")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(institutionalScopePort).findRolesActivosByUsuario(DOCENTE_SIN_IDENTIDAD);
        verify(institutionalScopePort).findDocenteIdByUsuario(DOCENTE_SIN_IDENTIDAD);
        verify(institutionalScopePort, never()).canDocenteAccessGrupo(DOCENTE_SIN_IDENTIDAD, GRUPO);
        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void multirrol_docente_coordinador_puede_usar_su_scope_de_coordinador() throws Exception {
        final MvcResult result = mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, bearer("docente-coordinador")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andReturn();

        assertEquals("Ana Perez", estudianteDeLaPrimeraFila(result));
        verify(institutionalScopePort).canCoordinadorAccessGrupo(DOCENTE_COORDINADOR, GRUPO);
    }

    @Test
    void multirrol_sin_identidad_docente_ni_scope_coordinador_recibe_403() throws Exception {
        mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, bearer("docente-coordinador-ajeno")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(institutionalScopePort).findDocenteIdByUsuario(DOCENTE_COORDINADOR_AJENO);
        verify(institutionalScopePort).canCoordinadorAccessGrupo(DOCENTE_COORDINADOR_AJENO, GRUPO);
        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void estudiante_recibe_403_forbidden_por_rbac_sin_llegar_al_caso_de_uso() throws Exception {
        mockMvc.perform(get(PATH, GRUPO).header(HttpHeaders.AUTHORIZATION, bearer("estudiante")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void sin_autenticacion_recibe_401_y_no_consulta_datos() throws Exception {
        mockMvc.perform(get(PATH, GRUPO))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void jwt_invalido_recibe_401_y_no_consulta_datos() throws Exception {
        mockMvc.perform(get(PATH, GRUPO).header(HttpHeaders.AUTHORIZATION, bearer("invalido")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    private static String estudianteDeLaPrimeraFila(final MvcResult result) throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            return workbook.getSheet("Asistencia").getRow(1).getCell(7).getStringCellValue();
        }
    }

    private static String bearer(final String token) {
        return "Bearer " + token;
    }

    private static Jwt jwtFor(final String token, final UUID userId, final List<String> institutionalRoleNames) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("keycloak-sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(List.of(AUDIENCE))
                .claim(USER_ID_CLAIM, userId.toString())
                .claim("resource_access", Map.of(API_CLIENT_ID, Map.of("roles", institutionalRoleNames)))
                .build();
    }

    private record TokenIdentity(UUID userId, List<String> roles) {
    }

    @TestConfiguration
    static class TestSupportConfig {

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(API_CLIENT_ID, USER_ID_CLAIM);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                final TokenIdentity identity = TOKENS.get(token);
                if (identity == null) {
                    throw new BadJwtException("Invalid test token.");
                }
                return jwtFor(token, identity.userId(), identity.roles());
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
