package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.interactor.SubirArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl.SubirArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.malwarescan.clamav.ClamAvMalwareScanAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.malwarescan.resilience.CircuitBreakerMalwareScanAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VAL H3 (REA-02/REA-06): con el antivirus caido (conexion rechazada), colgado (timeout de lectura) o con el circuit breaker
 * abierto, {@code POST /api/v1/archivos/subir} responde 503 con el contrato de error estandar y el codigo de catalogo
 * {@code ERR_ANTIVIRUS_NO_DISPONIBLE} (antes 500 {@code INTERNAL_APPLICATION_ERROR}). Sigue fallando cerrado: no se guardan bytes
 * ni metadata. Adaptador ClamAV real (socket) envuelto en el breaker real; controlador, caso de uso y manejador de errores reales.
 */
class SubirArchivoAntivirusNoDisponibleTest {

    private static final String CODIGO = "ERR_ANTIVIRUS_NO_DISPONIBLE";
    private static final String TEXTO_CATALOGO = "Texto del catalogo para el antivirus caido.";
    private static final int READ_TIMEOUT_MILLIS = 300;

    private final FileStoragePort almacen = mock(FileStoragePort.class);
    private final SoporteArchivoMetadataPort metadata = mock(SoporteArchivoMetadataPort.class);
    private final List<String> codigosConsultados = new ArrayList<>();

    private MockMvc mvc(final MalwareScanPort antivirus) {
        final AuthenticatedUserResolver identidad = UUID::randomUUID;
        final ArchivoController archivos = new ArchivoController(
                new SubirArchivoInteractor(new SubirArchivoUseCaseImpl(almacen, antivirus, metadata)),
                mock(DescargarArchivoInputPort.class), identidad);
        return MockMvcBuilders.standaloneSetup(archivos)
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> {
                    codigosConsultados.add(codigo);
                    return CODIGO.equals(codigo) ? Optional.of(TEXTO_CATALOGO) : Optional.empty();
                }))
                .build();
    }

    private static CircuitBreaker breaker() {
        return CircuitBreaker.of("malwareScan", CircuitBreakerConfig.custom()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(5)).build());
    }

    private static MalwareScanPort clamAv(final int puerto, final CircuitBreaker breaker) {
        return new CircuitBreakerMalwareScanAdapter(
                new ClamAvMalwareScanAdapter("127.0.0.1", puerto, 300, READ_TIMEOUT_MILLIS), breaker);
    }

    private static ResultActions subir(final MockMvc mvc) throws Exception {
        final byte[] pdf = "%PDF-1.4\nsoporte\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        return mvc.perform(multipart("/api/v1/archivos/subir")
                .file(new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", pdf)));
    }

    private static int puertoCerrado() throws IOException {
        try (ServerSocket sonda = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return sonda.getLocalPort();
        }
    }

    private void esperar503(final ResultActions resultado) throws Exception {
        resultado.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(CODIGO))
                .andExpect(jsonPath("$.message").value(TEXTO_CATALOGO))
                .andExpect(jsonPath("$.path").value("/api/v1/archivos/subir"))
                .andExpect(jsonPath("$.datos").doesNotExist())
                .andExpect(jsonPath("$.fileId").doesNotExist());
        assertTrue(codigosConsultados.contains(CODIGO), "El texto se busca en el catalogo por su codigo de mensaje");
        verifyNoInteractions(almacen, metadata);
    }

    @Test
    void antivirus_caido_conexion_rechazada_responde_503_y_no_almacena() throws Exception {
        esperar503(subir(mvc(clamAv(puertoCerrado(), breaker()))));
    }

    @Test
    void antivirus_que_no_responde_a_tiempo_responde_503_y_no_almacena() throws Exception {
        final AtomicReference<Socket> aceptado = new AtomicReference<>();
        try (ServerSocket colgado = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            final Thread aceptador = new Thread(() -> {
                try {
                    aceptado.set(colgado.accept());
                } catch (final IOException _) {
                    // el servidor se cierra al terminar la prueba
                }
            });
            aceptador.setDaemon(true);
            aceptador.start();

            final long inicio = System.nanoTime();
            esperar503(subir(mvc(clamAv(colgado.getLocalPort(), breaker()))));
            assertTrue(Duration.ofNanos(System.nanoTime() - inicio).toMillis() >= READ_TIMEOUT_MILLIS,
                    "La respuesta llega despues del timeout de lectura, no antes");
        } finally {
            final Socket socket = aceptado.get();
            if (socket != null) {
                socket.close();
            }
        }
    }

    @Test
    void con_el_breaker_abierto_responde_503_sin_intentar_conectar() throws Exception {
        final CircuitBreaker breaker = breaker();
        breaker.transitionToOpenState();
        final MalwareScanPort antivirus = mock(MalwareScanPort.class);

        esperar503(subir(mvc(new CircuitBreakerMalwareScanAdapter(antivirus, breaker))));

        verifyNoInteractions(antivirus);
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
    }

    @Test
    void fallos_repetidos_abren_el_breaker_y_todos_responden_503() throws Exception {
        final CircuitBreaker breaker = breaker();
        final MockMvc mvc = mvc(clamAv(puertoCerrado(), breaker));

        for (int intento = 0; intento < 3; intento++) {
            esperar503(subir(mvc));
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState(), "La semantica del breaker (REA-02) no cambia");
    }

    @Test
    void sin_texto_en_el_catalogo_usa_el_respaldo_del_codigo() throws Exception {
        final ArchivoController archivos = new ArchivoController(
                new SubirArchivoInteractor(new SubirArchivoUseCaseImpl(almacen, clamAv(puertoCerrado(), breaker()), metadata)),
                mock(DescargarArchivoInputPort.class), UUID::randomUUID);
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(archivos)
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

        subir(mvc).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(CODIGO))
                .andExpect(jsonPath("$.message").value(
                        "El antivirus no está disponible; el archivo no se guardó. Intente de nuevo en unos minutos."));
    }
}
