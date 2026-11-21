package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.interactor.SubirArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl.SubirArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.resilience.CircuitBreakerFileStorageAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REA-02 (VAL-oleada9): con el almacenamiento de objetos (MinIO) caido o con su circuit breaker abierto, subir un soporte responde
 * 503 con el codigo de catalogo {@code ERR_ALMACENAMIENTO_NO_DISPONIBLE}, como el antivirus (H3). Antes respondia 500
 * {@code INTERNAL_ERROR} (comprobado en el laboratorio el 2026-11-15 con {@code asis-datos-minio} detenido). La metadata nunca se
 * registra. Controlador, caso de uso, breaker y manejador de errores reales. La descarga tiene su propia prueba:
 * {@code DescargarArchivoAlmacenamientoNoDisponibleTest}.
 */
class ArchivoAlmacenamientoNoDisponibleTest {

    private static final String CODIGO = "ERR_ALMACENAMIENTO_NO_DISPONIBLE";
    private static final String TEXTO_CATALOGO = "Texto del catalogo para el almacenamiento caido.";

    private final SoporteArchivoMetadataPort metadata = mock(SoporteArchivoMetadataPort.class);
    private final MalwareScanPort antivirus = mock(MalwareScanPort.class);
    private final DescargarArchivoInputPort descarga = mock(DescargarArchivoInputPort.class);
    private final List<String> codigosConsultados = new ArrayList<>();

    private MockMvc mvc(final FileStoragePort almacen) {
        when(antivirus.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        final AuthenticatedUserResolver identidad = UUID::randomUUID;
        final ArchivoController archivos = new ArchivoController(
                new SubirArchivoInteractor(new SubirArchivoUseCaseImpl(almacen, antivirus, metadata)), descarga, identidad);
        return MockMvcBuilders.standaloneSetup(archivos)
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> {
                    codigosConsultados.add(codigo);
                    return CODIGO.equals(codigo) ? Optional.of(TEXTO_CATALOGO) : Optional.empty();
                }))
                .build();
    }

    private static FileStoragePort minioCaido() {
        final FileStoragePort almacen = mock(FileStoragePort.class);
        doThrow(new FileStoragePort.StorageUnavailableException("No fue posible almacenar el archivo en MinIO.",
                new java.net.ConnectException("Connection refused"))).when(almacen).store(any());
        return almacen;
    }

    private static ResultActions subir(final MockMvc mvc) throws Exception {
        final byte[] pdf = "%PDF-1.4\nsoporte\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        return mvc.perform(multipart("/api/v1/archivos/subir")
                .file(new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", pdf)));
    }

    private void esperar503(final ResultActions resultado, final String ruta) throws Exception {
        resultado.andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(CODIGO))
                .andExpect(jsonPath("$.message").value(TEXTO_CATALOGO))
                .andExpect(jsonPath("$.path").value(ruta));
        assertTrue(codigosConsultados.contains(CODIGO), "El texto se busca en el catalogo por su codigo de mensaje");
    }

    @Test
    void subir_con_minio_caido_responde_503_y_no_registra_metadata() throws Exception {
        esperar503(subir(mvc(minioCaido())), "/api/v1/archivos/subir");
        verifyNoInteractions(metadata);
    }

    @Test
    void subir_con_el_breaker_del_almacenamiento_abierto_responde_503_sin_llamar_a_minio() throws Exception {
        final CircuitBreaker breaker = CircuitBreaker.of("fileStorage", CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofMinutes(5)).build());
        breaker.transitionToOpenState();
        final FileStoragePort minio = mock(FileStoragePort.class);

        esperar503(subir(mvc(new CircuitBreakerFileStorageAdapter(minio, breaker))), "/api/v1/archivos/subir");

        verifyNoInteractions(minio, metadata);
    }
}
