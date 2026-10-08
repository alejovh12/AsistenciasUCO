package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RED causal LB-004B.2: STORAGE-004/005/006, INTEGRITY-001
 * (docs/work-items/LB-004-stateless-serverless-readiness/TEST_PLAN.md).
 *
 * <p>Certifica el borde REAL {@code MinioFileStorageAdapter -> MinIO} contra el servicio local
 * provisto por {@code infra/files/compose.yaml} (servicio {@code minio}, bucket
 * {@code asistencias-soportes}). No mockea el SDK: dos clientes MinIO independientes se usan
 * deliberadamente para demostrar STATELESS-001/STORAGE-006 (dos "instancias" leen el mismo
 * objeto). Requiere el stack de {@code infra/files/compose.yaml} corriendo; sin el contenedor disponible
 * esta prueba FALLA con un error de conexion claro, nunca se omite silenciosamente.</p>
 */
@Tag("integration")
class MinioFileStorageAdapterIT {

    private static final String ENDPOINT = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://localhost:9000");
    private static final String ACCESS_KEY = System.getenv().getOrDefault("MINIO_ACCESS_KEY", "asistencias-minio-dev");
    private static final String SECRET_KEY = System.getenv().getOrDefault("MINIO_SECRET_KEY", "asistencias-minio-dev-secret");
    private static final String BUCKET = System.getenv().getOrDefault("MINIO_BUCKET", "asistencias-soportes");

    private final List<String> createdFileIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        final FileStoragePort adapter = newAdapter();
        for (final String fileId : createdFileIds) {
            try {
                adapter.delete(fileId);
            } catch (final RuntimeException ignored) {
                // best-effort: el objeto pudo no haberse llegado a crear en un test fallido
            }
        }
        createdFileIds.clear();
    }

    // STORAGE-004
    @Test
    void store_y_read_recuperan_los_mismos_bytes_y_metadata() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] content = "%PDF-1.4 contenido de prueba MinioFileStorageAdapterIT".getBytes();
        final FileStoragePort.StoredObjectMetadata metadata = metadataFor(content, "soporte.pdf");

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, content, metadata));
        final FileStoragePort.StoredObject read = adapter.read(fileId);

        assertArrayEquals(content, read.content());
        assertEquals(metadata.ownerSubject(), read.metadata().ownerSubject());
        assertEquals(metadata.originalFilename(), read.metadata().originalFilename());
        assertEquals(metadata.contentType(), read.metadata().contentType());
        // INTEGRITY-001: checksum sobrevive el round-trip de metadata tecnica del objeto.
        assertEquals(metadata.checksumSha256(), read.metadata().checksumSha256());
    }

    @Test
    void exists_refleja_presencia_real_del_objeto() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        assertFalse(adapter.exists(fileId));

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, "contenido".getBytes(), metadataFor("contenido".getBytes(), "x.pdf")));

        assertTrue(adapter.exists(fileId));
    }

    @Test
    void read_de_fileId_inexistente_lanza_object_not_found() {
        final FileStoragePort adapter = newAdapter();

        assertThrows(FileStoragePort.ObjectNotFoundException.class, () -> adapter.read(UUID.randomUUID().toString()));
    }

    @Test
    void delete_elimina_el_objeto_y_exists_pasa_a_false() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, "contenido".getBytes(), metadataFor("contenido".getBytes(), "x.pdf")));

        adapter.delete(fileId);

        assertFalse(adapter.exists(fileId));
    }

    // STORAGE-005: restart simulado — un adapter/cliente NUEVO (nueva "instancia") lee el objeto
    // creado por el adapter original.
    @Test
    void un_nuevo_adapter_recupera_el_objeto_tras_un_restart_simulado() {
        final String fileId = newTrackedFileId();
        final byte[] content = "contenido persistente".getBytes();
        newAdapter().store(new FileStoragePort.StoreObjectCommand(fileId, content, metadataFor(content, "x.pdf")));

        final FileStoragePort adapterTrasRestart = newAdapter();
        final FileStoragePort.StoredObject read = adapterTrasRestart.read(fileId);

        assertArrayEquals(content, read.content());
    }

    // STORAGE-006 / STATELESS-001: dos adapters/clientes independientes contra el mismo MinIO;
    // A escribe, B lee el mismo objeto.
    @Test
    void dos_adapters_independientes_contra_el_mismo_minio_leen_el_mismo_objeto() {
        final FileStoragePort adapterA = newAdapter();
        final FileStoragePort adapterB = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] content = "bytes escritos por instancia A".getBytes();

        adapterA.store(new FileStoragePort.StoreObjectCommand(fileId, content, metadataFor(content, "x.pdf")));
        final FileStoragePort.StoredObject readByB = adapterB.read(fileId);

        assertArrayEquals(content, readByB.content());
    }

    private String newTrackedFileId() {
        final String fileId = UUID.randomUUID().toString();
        createdFileIds.add(fileId);
        return fileId;
    }

    private FileStoragePort.StoredObjectMetadata metadataFor(final byte[] content, final String filename) {
        return new FileStoragePort.StoredObjectMetadata(
                UUID.randomUUID().toString(),
                filename,
                "application/pdf",
                content.length,
                content.length,
                false,
                null,
                co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator.sha256(content),
                Instant.now()
        );
    }

    private static FileStoragePort newAdapter() {
        final MinioClient client = MinioClient.builder()
                .endpoint(ENDPOINT)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .build();
        return new MinioFileStorageAdapter(client, BUCKET);
    }
}


