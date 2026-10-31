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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regresión positiva: ADMINISTRADOR conserva el acceso institucional al reporte. */
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
        ReporteAsistenciaAdministradorAccessHttpTest.TestSupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class ReporteAsistenciaAdministradorAccessHttpTest {

    private static final String AUDIENCE = "asistencias-api";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PATH = "/api/v1/grupos/{grupoId}/reportes/asistencia-excel";

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ADMINISTRADOR = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID ADMINISTRADOR_INACTIVO = UUID.fromString("77777777-7777-7777-7777-777777777777");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstitutionalScopePort institutionalScopePort;

    @MockitoBean
    private ReporteAsistenciaQueryPort reporteAsistenciaQueryPort;

    @BeforeEach
    void setUp() {
        when(institutionalScopePort.findRolesActivosByUsuario(ADMINISTRADOR)).thenReturn(Set.of(InstitutionalRole.ADMINISTRADOR));
        when(institutionalScopePort.findRolesActivosByUsuario(ADMINISTRADOR_INACTIVO)).thenReturn(Set.of(InstitutionalRole.ADMINISTRADOR));
        when(institutionalScopePort.isAdministradorActivo(ADMINISTRADOR)).thenReturn(true);
        when(institutionalScopePort.isAdministradorActivo(ADMINISTRADOR_INACTIVO)).thenReturn(false);
        when(institutionalScopePort.findDocenteIdByUsuario(ADMINISTRADOR)).thenReturn(Optional.empty());
        when(reporteAsistenciaQueryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(new ReporteAsistenciaRow(
                "G01", "Grupo 01", 1, "Sesion 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", "Ana Perez", "ana@uco.edu.co", Boolean.TRUE, null)));
    }

    @Test
    void administrador_activo_conserva_acceso_institucional() throws Exception {
        final MvcResult result = mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer administrador"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(XLSX))
                .andReturn();

        assertEquals("Ana Perez", estudianteDeLaPrimeraFila(result));
        verify(institutionalScopePort, never()).canDocenteAccessGrupo(ADMINISTRADOR, GRUPO);
        verify(institutionalScopePort).findRolesActivosByUsuario(ADMINISTRADOR);
        verify(institutionalScopePort).isAdministradorActivo(ADMINISTRADOR);
        verify(reporteAsistenciaQueryPort).consultarReporteAsistenciaGrupo(GRUPO);
    }

    @Test
    void administrador_inactivo_recibe_403_sin_consultar_el_reporte() throws Exception {
        mockMvc.perform(get(PATH, GRUPO)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer administrador-inactivo"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));

        verify(institutionalScopePort).findRolesActivosByUsuario(ADMINISTRADOR_INACTIVO);
        verify(institutionalScopePort).isAdministradorActivo(ADMINISTRADOR_INACTIVO);
        verify(reporteAsistenciaQueryPort, never()).consultarReporteAsistenciaGrupo(org.mockito.ArgumentMatchers.any());
    }

    private static String estudianteDeLaPrimeraFila(final MvcResult result) throws Exception {
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))) {
            return workbook.getSheet("Asistencia").getRow(1).getCell(7).getStringCellValue();
        }
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
                if (!"administrador".equals(token) && !"administrador-inactivo".equals(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .notBefore(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim(USER_ID_CLAIM, ("administrador".equals(token) ? ADMINISTRADOR : ADMINISTRADOR_INACTIVO).toString())
                        .claim("resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of("ADMINISTRADOR"))))
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
