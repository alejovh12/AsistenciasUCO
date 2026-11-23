package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.interactor.DescargarArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl.DescargarArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.EstadoSoporte;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RelacionAcceso;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.SoporteArchivoAutorizado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.resilience.CircuitBreakerFileStorageAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REA-02 (revision final P3): con MinIO caido o con el breaker {@code fileStorage} abierto, DESCARGAR un soporte responde 503
 * {@code ERR_ALMACENAMIENTO_NO_DISPONIBLE} como subirlo (antes respondia 500 {@code INTERNAL_ERROR}). Controlador, caso de uso,
 * breaker y manejador de errores reales.
 */
class DescargarArchivoAlmacenamientoNoDisponibleTest {

    private static final String CODIGO = "ERR_ALMACENAMIENTO_NO_DISPONIBLE";
    private static final String TEXTO_CATALOGO = "Texto del catalogo para el almacenamiento caido.";

    private final SoporteArchivoMetadataPort metadata = mock(SoporteArchivoMetadataPort.class);

    private MockMvc mvc(final FileStoragePort almacen, final UUID fileId) {
        when(metadata.consultarAutorizado(any(), any())).thenReturn(Optional.of(new SoporteArchivoAutorizado(
                fileId, EstadoSoporte.ATTACHED, "soporte.pdf", "application/pdf", 10L, "x", Instant.now(), Instant.now(),
                null, RelacionAcceso.PROPIETARIO)));
        final AuthenticatedUserResolver identidad = UUID::randomUUID;
        final ArchivoController archivos = new ArchivoController(
                mock(SubirArchivoInputPort.class),
                new DescargarArchivoInteractor(new DescargarArchivoUseCaseImpl(almacen, metadata)), identidad);
        return MockMvcBuilders.standaloneSetup(archivos)
                .setControllerAdvice(new GlobalExceptionHandler(
                        codigo -> CODIGO.equals(codigo) ? Optional.of(TEXTO_CATALOGO) : Optional.empty()))
                .build();
    }

    @Test
    void descargar_con_minio_caido_responde_503() throws Exception {
        final UUID fileId = UUID.randomUUID();
        final FileStoragePort almacen = mock(FileStoragePort.class);
        when(almacen.read(anyString())).thenThrow(new FileStoragePort.StorageUnavailableException(
                "No fue posible leer el archivo en MinIO.", new java.net.ConnectException("Connection refused")));

        mvc(almacen, fileId).perform(get("/api/v1/archivos/" + fileId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(CODIGO))
                .andExpect(jsonPath("$.message").value(TEXTO_CATALOGO));
    }

    @Test
    void descargar_con_el_breaker_del_almacenamiento_abierto_responde_503_sin_llamar_a_minio() throws Exception {
        final UUID fileId = UUID.randomUUID();
        final CircuitBreaker breaker = CircuitBreaker.of("fileStorage", CircuitBreakerConfig.custom()
                .waitDurationInOpenState(Duration.ofMinutes(5)).build());
        breaker.transitionToOpenState();
        final FileStoragePort minio = mock(FileStoragePort.class);

        mvc(new CircuitBreakerFileStorageAdapter(minio, breaker), fileId).perform(get("/api/v1/archivos/" + fileId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(CODIGO));

        verifyNoInteractions(minio);
    }
}
