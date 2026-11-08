package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.EjecutarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.ValidarCargaUsuariosInputPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EstadoFilaResultado;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.EstadoFilaValidacion;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.FilaResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.FilaValidacionCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoCargaDTO;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.primaryports.dto.ResultadoValidacionCargaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CargaUsuariosControllerTest {

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String CLAVE_SECRETA_TEST = "ClaveSecretaUnica123!";

    private final ValidarCargaUsuariosInputPort validarInputPort = mock(ValidarCargaUsuariosInputPort.class);
    private final EjecutarCargaUsuariosInputPort ejecutarInputPort = mock(EjecutarCargaUsuariosInputPort.class);
    private final AuthenticatedUserResolver identidad = () -> ACTOR_ID;
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofMinutes(1), 100, Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("UTC")));

    private final CoordinadorCargasController coordinadorController = new CoordinadorCargasController(validarInputPort, ejecutarInputPort, identidad, limiter);
    private final DecanoCargasController decanoController = new DecanoCargasController(validarInputPort, ejecutarInputPort, identidad, limiter);
    private final AdminCargasController adminController = new AdminCargasController(validarInputPort, ejecutarInputPort, identidad, limiter);

    private MockMvc mvcCoordinador;
    private MockMvc mvcDecano;
    private MockMvc mvcAdmin;

    private ListAppender<ILoggingEvent> logs;
    private Logger raiz;

    @BeforeEach
    void setup() {
        final GlobalExceptionHandler advice = new GlobalExceptionHandler(codigo -> Optional.empty());
        mvcCoordinador = MockMvcBuilders.standaloneSetup(coordinadorController).setControllerAdvice(advice).build();
        mvcDecano = MockMvcBuilders.standaloneSetup(decanoController).setControllerAdvice(advice).build();
        mvcAdmin = MockMvcBuilders.standaloneSetup(adminController).setControllerAdvice(advice).build();

        raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logs = new ListAppender<>();
        logs.start();
        raiz.addAppender(logs);
        raiz.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        raiz.detachAppender(logs);
    }

    @Test
    @DisplayName("D5-C01: Descargar plantillas CSV con BOM, delimitador ';' y Content-Disposition")
    void descargarPlantillasCsv() throws Exception {
        // Coordinador -> plantilla docentes
        final var resCoord = mvcCoordinador.perform(get("/api/v1/coordinador/cargas/plantilla"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-carga-docentes.csv\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertEquals((byte) 0xEF, resCoord[0]);
        assertEquals((byte) 0xBB, resCoord[1]);
        assertEquals((byte) 0xBF, resCoord[2]);
        final String textoCoord = new String(resCoord, StandardCharsets.UTF_8);
        assertTrue(textoCoord.contains("tipo_documento;numero_documento"));
        assertTrue(textoCoord.contains("codigo_tipo_vinculacion"));

        // Decano -> plantilla coordinadores
        final var resDecano = mvcDecano.perform(get("/api/v1/decano/cargas/plantilla"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-carga-coordinadores.csv\""))
                .andReturn().getResponse().getContentAsByteArray();
        final String textoDecano = new String(resDecano, StandardCharsets.UTF_8);
        assertTrue(textoDecano.contains("codigo_programa"));

        // Admin -> plantilla decanos
        final var resAdmin = mvcAdmin.perform(get("/api/v1/admin/cargas/plantilla"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-carga-decanos.csv\""))
                .andReturn().getResponse().getContentAsByteArray();
        final String textoAdmin = new String(resAdmin, StandardCharsets.UTF_8);
        assertTrue(textoAdmin.contains("codigo_facultad"));
    }

    @Test
    @DisplayName("D5-C02: Validacion y Ejecucion retornan ApiDataResponse sin exponer contrasenas")
    void validacionYEjecucionRetornanApiDataResponse() throws Exception {
        final String csv = "tipo_documento;numero_documento;primer_nombre;primer_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                + "CC;1017123456;Carlos;Gomez;carlos@uco.edu.co;" + CLAVE_SECRETA_TEST + ";TC;ING-SIS\r\n";
        final MockMultipartFile archivo = new MockMultipartFile("archivo", "docentes.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(validarInputPort.validar(any())).thenReturn(new ResultadoValidacionCargaDTO(
                "huella123", PerfilDestinoCarga.DOCENTE, 1, 1, 0, 0,
                List.of(new FilaValidacionCargaDTO(2, "1017123456", "carlos@uco.edu.co", EstadoFilaValidacion.VALIDA, List.of()))
        ));

        when(ejecutarInputPort.ejecutar(any())).thenReturn(new ResultadoCargaDTO(
                1, 1, 0, 0, 0,
                List.of(new FilaResultadoCargaDTO(2, "1017123456", "carlos@uco.edu.co", EstadoFilaResultado.CREADA, UUID.randomUUID(), List.of())),
                "Carga procesada: 1 creadas, 0 ya existentes, 0 con error."
        ));

        mvcCoordinador.perform(multipart("/api/v1/coordinador/cargas/validacion").file(archivo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.huella").value("huella123"))
                .andExpect(jsonPath("$.datos.totalFilas").value(1))
                .andExpect(jsonPath("$.datos.filasValidas").value(1));

        mvcCoordinador.perform(multipart("/api/v1/coordinador/cargas").file(archivo).param("huella", "huella123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.totalFilas").value(1))
                .andExpect(jsonPath("$.datos.creadas").value(1));

        // OBS-12: Comprobar que la clave secreta nunca aparece en logs
        for (final ILoggingEvent log : logs.list) {
            assertFalse(log.getFormattedMessage().contains(CLAVE_SECRETA_TEST),
                    "OBS-12: La clave inicial nunca debe imprimirse en los logs.");
        }
    }

    @Test
    @DisplayName("D5-C03: Rate limit 429 CMA_014 al superar 3 cargas por minuto")
    void rateLimitLanza429() throws Exception {
        final MockMultipartFile archivo = new MockMultipartFile("archivo", "test.csv", "text/csv", "dummy".getBytes());
        when(validarInputPort.validar(any())).thenReturn(new ResultadoValidacionCargaDTO(
                "h", PerfilDestinoCarga.DECANO, 1, 1, 0, 0, List.of()
        ));

        // Peticiones 1, 2, 3 pasan
        mvcAdmin.perform(multipart("/api/v1/admin/cargas/validacion").file(archivo)).andExpect(status().isOk());
        mvcAdmin.perform(multipart("/api/v1/admin/cargas/validacion").file(archivo)).andExpect(status().isOk());
        mvcAdmin.perform(multipart("/api/v1/admin/cargas/validacion").file(archivo)).andExpect(status().isOk());

        // Peticion 4 es rechazada con 429 CMA_014
        mvcAdmin.perform(multipart("/api/v1/admin/cargas/validacion").file(archivo))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("CMA_014"));
    }

    @Test
    @DisplayName("D5-C04: Auditoria @AuditableOperation presente en ejecutar y ausente en validacion")
    void auditoriaPresenteSoloEnEjecutar() throws Exception {
        // Coordinador
        final Method mCoordVal = CoordinadorCargasController.class.getMethod("validar", org.springframework.web.multipart.MultipartFile.class);
        assertNull(mCoordVal.getAnnotation(AuditableOperation.class), "La validacion no debe auditarse");
        final Method mCoordEj = CoordinadorCargasController.class.getMethod("ejecutar", org.springframework.web.multipart.MultipartFile.class, String.class);
        final AuditableOperation aCoord = mCoordEj.getAnnotation(AuditableOperation.class);
        assertNotNull(aCoord);
        assertEquals("CARGA_MASIVA_DOCENTE", aCoord.action());

        // Decano
        final Method mDecanoVal = DecanoCargasController.class.getMethod("validar", org.springframework.web.multipart.MultipartFile.class);
        assertNull(mDecanoVal.getAnnotation(AuditableOperation.class));
        final Method mDecanoEj = DecanoCargasController.class.getMethod("ejecutar", org.springframework.web.multipart.MultipartFile.class, String.class);
        final AuditableOperation aDecano = mDecanoEj.getAnnotation(AuditableOperation.class);
        assertNotNull(aDecano);
        assertEquals("CARGA_MASIVA_COORDINADOR", aDecano.action());

        // Admin
        final Method mAdminVal = AdminCargasController.class.getMethod("validar", org.springframework.web.multipart.MultipartFile.class);
        assertNull(mAdminVal.getAnnotation(AuditableOperation.class));
        final Method mAdminEj = AdminCargasController.class.getMethod("ejecutar", org.springframework.web.multipart.MultipartFile.class, String.class);
        final AuditableOperation aAdmin = mAdminEj.getAnnotation(AuditableOperation.class);
        assertNotNull(aAdmin);
        assertEquals("CARGA_MASIVA_DECANO", aAdmin.action());
        assertArrayEquals(new String[]{"totalFilas", "creadas", "yaExistentes", "conError"}, aAdmin.metadataResponseFields());
    }
}
