package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.malwarescan.clamav.ClamAvMalwareScanAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.storage.minio.MinioStorageAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.ArchivoWiringConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RED causal LB-004B.2: E2E backend real, sin frontend
 * (docs/work-items/LB-004-stateless-serverless-readiness/E2E_PLAN.md §FASE 1,
 * spec §19 "Integration tests reales").
 *
 * <pre>
 * HTTP upload (ArchivoController.subirArchivo)
 *   -&gt; validacion de contenido (ContentSecurityValidator)
 *   -&gt; ClamAV real (ClamAvMalwareScanAdapter)
 *   -&gt; MinIO real (MinioFileStorageAdapter)
 *   -&gt; HTTP download (ArchivoController.descargarArchivo)
 *   -&gt; mismos bytes originales
 * </pre>
 *
 * <p>Contexto Spring minimo: solo la Composition Root de storage/malware-scan + wiring de
 * Application + el controller real. No requiere SQL Server ni Keycloak (fuera del alcance de
 * esta microfase); si requiere {@code docker compose up -d minio clamav} corriendo.</p>
 */
@Tag("integration")
@SpringBootTest(
        classes = {
                MinioStorageAdapterConfiguration.class,
                ClamAvMalwareScanAdapterConfiguration.class,
                ArchivoWiringConfiguration.class,
                ArchivoController.class,
                ArchivoUploadDownloadFlowIT.TestSupportConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "app.adapters.storage.provider=minio",
        "app.providers.minio.endpoint=${MINIO_ENDPOINT:http://localhost:9000}",
        "app.providers.minio.access-key=${MINIO_ACCESS_KEY:asistencias-minio-dev}",
        "app.providers.minio.secret-key=${MINIO_SECRET_KEY:asistencias-minio-dev-secret}",
        "app.providers.minio.bucket=${MINIO_BUCKET:asistencias-soportes}",
        "app.providers.clamav.host=${CLAMAV_HOST:localhost}",
        "app.providers.clamav.port=${CLAMAV_PORT:3310}",
        "app.providers.clamav.connect-timeout-millis=5000",
        "app.providers.clamav.read-timeout-millis=15000"
})
class ArchivoUploadDownloadFlowIT {

    @Autowired
    private ArchivoController archivoController;

    @Test
    void flujo_completo_upload_validacion_clamav_minio_download_devuelve_los_mismos_bytes() {
        final byte[] originalContent = withPdfMagic("HU014 soporte de prueba E2E LB-004B.2");
        final MockMultipartFile archivo = new MockMultipartFile("archivo", "soporte.pdf", "application/pdf", originalContent);

        final ResponseEntity<ApiDataResponse<Map<String, Object>>> uploadResponse = archivoController.subirArchivo(archivo);
        assertEquals(HttpStatus.CREATED, uploadResponse.getStatusCode());
        final UUID fileId = (UUID) uploadResponse.getBody().datos().get("fileId");

        final ResponseEntity<byte[]> downloadResponse = archivoController.descargarArchivo(fileId.toString());

        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode());
        assertArrayEquals(originalContent, downloadResponse.getBody());
    }

    // Nota: MALWARE-001 (EICAR -> rechazado contra ClamAV real) se certifica en
    // ClamAvMalwareScanAdapterIT con la firma EICAR pura, no aqui. Se comprobo empiricamente que
    // este ClamAV (imagen clamav/clamav:1.5-debian13-slim) detecta la firma EICAR solo cerca del
    // inicio del stream: un PDF valido (%PDF- en offset 0) con EICAR embebido despues de los
    // magic bytes NO es detectado por este motor (verificado con clamdscan dentro del contenedor:
    // "%PDF-" + EICAR -> OK; EICAR puro -> FOUND). Como el formato PDF exige su firma en el
    // offset 0, no existe un fixture "PDF valido + EICAR" honesto para este motor especifico; la
    // ruta de rechazo por malware ya queda cubierta en el limite real Adapter-ClamAV
    // (ClamAvMalwareScanAdapterIT) y en la orquestacion (SubirArchivoUseCaseImplTest, con
    // MalwareScanPort simulado). Ver RISKS.md R-LB004-025.

    // Caso negativo: fileId inexistente en MinIO real -> 404, no revela existencia.
    @Test
    void descarga_de_fileId_inexistente_retorna_resource_not_found() {
        assertThrows(ResourceNotFoundException.class,
                () -> archivoController.descargarArchivo(UUID.randomUUID().toString()));
    }

    private static byte[] withPdfMagic(final String payload) {
        final byte[] magic = {0x25, 0x50, 0x44, 0x46, 0x2D};
        final byte[] payloadBytes = payload.getBytes();
        final byte[] content = new byte[magic.length + payloadBytes.length];
        System.arraycopy(magic, 0, content, 0, magic.length);
        System.arraycopy(payloadBytes, 0, content, magic.length, payloadBytes.length);
        return content;
    }

    @TestConfiguration
    static class TestSupportConfig {

        @Bean
        AuthenticatedUserResolver authenticatedUserResolver() {
            final UUID fixedUser = UUID.randomUUID();
            return () -> fixedUser;
        }
    }
}


