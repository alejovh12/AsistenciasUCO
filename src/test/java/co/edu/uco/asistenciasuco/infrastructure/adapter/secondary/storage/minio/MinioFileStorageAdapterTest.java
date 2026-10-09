package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.errors.ServerException;
import io.minio.messages.ErrorResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QUALITY-PR15 F01–F03 a nivel unitario: traduccion del SDK MinIO al {@link FileStoragePort}.
 * El SDK esta simulado; el comportamiento real del provider lo certifica {@code MinioFileStorageAdapterIT}.
 */
class MinioFileStorageAdapterTest {

    private static final String BUCKET = "soportes-privados";
    private static final byte[] PDF = "%PDF-1.4 soporte".getBytes(StandardCharsets.US_ASCII);

    private final MinioClient client = mock(MinioClient.class);
    private final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(client, BUCKET);

    @Test
    void configuracion_invalida_se_rechaza_al_construir() {
        assertThrows(NullPointerException.class, () -> new MinioFileStorageAdapter(null, BUCKET));
        assertThrows(IllegalArgumentException.class, () -> new MinioFileStorageAdapter(client, null));
        assertThrows(IllegalArgumentException.class, () -> new MinioFileStorageAdapter(client, " "));
    }

    @Test
    void store_escribe_bytes_y_metadata_tecnica_bajo_la_clave_derivada_del_file_id() throws Exception {
        final String fileId = UUID.randomUUID().toString();
        final FileStoragePort.StoredObjectMetadata metadata = metadata(true, "DEFLATE");

        final FileStoragePort.StoredObject stored =
                adapter.store(new FileStoragePort.StoreObjectCommand(fileId, PDF, metadata));

        final ArgumentCaptor<PutObjectArgs> put = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(put.capture());
        assertEquals(BUCKET, put.getValue().bucket());
        assertEquals("soportes/" + fileId, put.getValue().object());
        assertEquals("application/pdf", put.getValue().contentType().toString());
        assertEquals(PDF.length, put.getValue().objectSize());
        final Http.Headers userMetadata = put.getValue().userMetadata();
        assertEquals(metadata.ownerSubject(), meta(userMetadata, "owner-subject"));
        assertEquals("soporte.pdf", meta(userMetadata, "original-filename"));
        assertEquals("true", meta(userMetadata, "compressed"));
        assertEquals("DEFLATE", meta(userMetadata, "compression-algorithm"));
        assertEquals(metadata.checksumSha256(), meta(userMetadata, "checksum-sha256"));
        assertEquals(new FileStoragePort.StoredObject(fileId, PDF, metadata), stored);
    }

    @Test
    void store_sin_algoritmo_guarda_cadena_vacia_y_no_null() throws Exception {
        adapter.store(new FileStoragePort.StoreObjectCommand("f", PDF, metadata(false, null)));

        final ArgumentCaptor<PutObjectArgs> put = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client).putObject(put.capture());
        assertEquals("", meta(put.getValue().userMetadata(), "compression-algorithm"));
        assertEquals("false", meta(put.getValue().userMetadata(), "compressed"));
    }

    @Test
    void fallo_del_sdk_al_almacenar_no_confirma_la_operacion() throws Exception {
        final MinioException transport = new MinioException("connection reset");
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(transport);

        final FileStoragePort.StorageUnavailableException error = assertThrows(
                FileStoragePort.StorageUnavailableException.class,
                () -> adapter.store(new FileStoragePort.StoreObjectCommand("f", PDF, metadata(false, null))));
        assertSame(transport, error.getCause());
    }

    @Test
    void read_restaura_bytes_y_metadata_del_objeto() throws Exception {
        final Map<String, String> userMetadata = userMetadata("true", "DEFLATE");
        stubObject(PDF.length, userMetadata, new ByteArrayInputStream(PDF));

        final FileStoragePort.StoredObject stored = adapter.read("f-1");

        assertArrayEquals(PDF, stored.content());
        assertEquals("f-1", stored.fileId());
        assertEquals("owner-1", stored.metadata().ownerSubject());
        assertEquals("soporte.pdf", stored.metadata().originalFilename());
        assertEquals("application/pdf", stored.metadata().contentType());
        assertEquals(64L, stored.metadata().originalSize());
        assertEquals(PDF.length, stored.metadata().storedSize());
        assertTrue(stored.metadata().compressed());
        assertEquals("DEFLATE", stored.metadata().compressionAlgorithm());
        assertEquals(Instant.parse("2026-10-08T12:00:00Z"), stored.metadata().uploadedAt());
    }

    @Test
    void read_sin_algoritmo_ni_tamano_original_usa_valores_neutros() throws Exception {
        final Map<String, String> userMetadata = userMetadata("false", "");
        userMetadata.remove("original-size");
        stubObject(PDF.length, userMetadata, new ByteArrayInputStream(PDF));

        final FileStoragePort.StoredObject stored = adapter.read("f-1");

        assertNull(stored.metadata().compressionAlgorithm());
        assertEquals(0L, stored.metadata().originalSize());
        assertFalse(stored.metadata().compressed());
    }

    @Test
    void objeto_inexistente_se_traduce_a_not_found() throws Exception {
        doThrow(errorResponse("NoSuchKey")).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.ObjectNotFoundException.class, () -> adapter.read("f-1"));

        doThrow(errorResponse("NoSuchObject")).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.ObjectNotFoundException.class, () -> adapter.read("f-1"));
        verify(client, never()).getObject(any(GetObjectArgs.class));
    }

    @Test
    void error_de_autorizacion_del_provider_no_se_enmascara_como_not_found() throws Exception {
        doThrow(errorResponse("AccessDenied")).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("f-1"));

        doThrow(new ServerException("bad gateway", 502, null)).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("f-1"));
    }

    @Test
    void fallo_de_transporte_al_leer_el_cuerpo_es_error_tecnico() throws Exception {
        final InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("stream reset");
            }
        };
        stubObject(PDF.length, userMetadata("false", ""), broken);

        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("f-1"));
    }

    @Test
    void cuerpo_mas_corto_que_el_tamano_declarado_es_inconsistente() throws Exception {
        stubObject(PDF.length + 10L, userMetadata("false", ""), new ByteArrayInputStream(PDF));

        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("f-1"));
    }

    @Test
    void metadata_corrupta_no_se_entrega_como_objeto_valido() throws Exception {
        final Map<String, String> userMetadata = userMetadata("false", "");
        userMetadata.put("uploaded-at", "ayer");
        stubObject(PDF.length, userMetadata, new ByteArrayInputStream(PDF));

        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read("f-1"));
    }

    @Test
    void exists_distingue_presente_ausente_y_fallo_tecnico() throws Exception {
        doReturn(mock(StatObjectResponse.class)).when(client).statObject(any(StatObjectArgs.class));
        assertTrue(adapter.exists("f-1"));

        doThrow(errorResponse("NoSuchKey")).when(client).statObject(any(StatObjectArgs.class));
        assertFalse(adapter.exists("f-1"));

        doThrow(errorResponse("AccessDenied")).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.exists("f-1"));

        doThrow(new MinioException("timeout")).when(client).statObject(any(StatObjectArgs.class));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.exists("f-1"));
    }

    @Test
    void delete_elimina_la_clave_derivada_y_traduce_fallos() throws Exception {
        adapter.delete("f-1");
        final ArgumentCaptor<RemoveObjectArgs> remove = ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client).removeObject(remove.capture());
        assertEquals(BUCKET, remove.getValue().bucket());
        assertEquals("soportes/f-1", remove.getValue().object());

        doThrow(new MinioException("down")).when(client).removeObject(any(RemoveObjectArgs.class));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.delete("f-1"));
    }

    @Test
    void identificadores_nulos_se_rechazan_sin_contactar_el_provider() {
        assertThrows(NullPointerException.class, () -> adapter.read(null));
        assertThrows(NullPointerException.class, () -> adapter.exists(null));
        assertThrows(NullPointerException.class, () -> adapter.delete(null));
        assertThrows(NullPointerException.class, () -> adapter.store(null));
    }

    private void stubObject(final long size, final Map<String, String> userMetadata, final InputStream body)
            throws Exception {
        final StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(size);
        when(stat.contentType()).thenReturn("application/pdf");
        when(stat.userMetadata()).thenReturn(new Http.Headers(userMetadata));
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(stat);
        when(client.getObject(any(GetObjectArgs.class)))
                .thenReturn(new GetObjectResponse(Headers.of(), BUCKET, "", "soportes/f-1", body));
    }

    private static Map<String, String> userMetadata(final String compressed, final String algorithm) {
        final Map<String, String> values = new HashMap<>();
        values.put("owner-subject", "owner-1");
        values.put("original-filename", "soporte.pdf");
        values.put("original-size", "64");
        values.put("compressed", compressed);
        values.put("compression-algorithm", algorithm);
        values.put("checksum-sha256", "c".repeat(64));
        values.put("uploaded-at", "2026-10-08T12:00:00Z");
        return values;
    }

    private static FileStoragePort.StoredObjectMetadata metadata(final boolean compressed, final String algorithm) {
        return new FileStoragePort.StoredObjectMetadata(UUID.randomUUID().toString(), "soporte.pdf",
                "application/pdf", PDF.length, PDF.length, compressed, algorithm, "b".repeat(64),
                Instant.parse("2026-10-08T12:00:00Z"));
    }

    /** El SDK serializa la metadata de usuario con el prefijo S3 {@code x-amz-meta-}. */
    private static String meta(final Http.Headers headers, final String key) {
        final String prefixed = headers.getFirst("x-amz-meta-" + key);
        return prefixed != null ? prefixed : headers.getFirst(key);
    }

    private static ErrorResponseException errorResponse(final String code) {
        return new ErrorResponseException(
                new ErrorResponse(code, "simulated", BUCKET, "soportes/f-1", "/", "req", "host"), null, null);
    }
}
