package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba candidata RED, QUALITY-PR15 F04.
 * Mock del SDK (no demuestra comportamiento de MinIO real).
 * Verifica que una lectura inesperadamente grande NO se consuma completa en memoria.
 *
 * <p>Correccion tester 2026-10-08: la version anterior mockeaba {@code GetObjectResponse}; Mockito
 * anulaba {@code transferTo} (consumo 0) y la excepcion provenia de metadata nula, no del
 * presupuesto (pasaba en vacio). Ahora el cuerpo es un stream real que cuenta bytes y la metadata
 * es valida, de modo que el unico motivo de fallo posible es la lectura no acotada.</p>
 */
class MinioReadBudgetTest {

    private static final int MAX_ORIGINAL_BYTES = 5 * 1024 * 1024;
    private static final int OVERSIZED_OBJECT_BYTES = MAX_ORIGINAL_BYTES + 256 * 1024;
    private static final int READ_SLACK_BYTES = 8_192;

    @Test
    void lectura_del_objeto_sobre_el_presupuesto_aborta_sin_consumirlo_completo() throws Exception {
        final CountingStream body = new CountingStream(OVERSIZED_OBJECT_BYTES);
        final MinioFileStorageAdapter adapter = adapterServing(OVERSIZED_OBJECT_BYTES, body);

        assertThrows(FileStoragePort.StorageUnavailableException.class,
                () -> adapter.read(UUID.randomUUID().toString()));
        assertTrue(body.consumed() <= MAX_ORIGINAL_BYTES + READ_SLACK_BYTES,
                "No debe leer el objeto entero antes de rechazarlo; bytes consumidos: " + body.consumed());
    }

    @Test
    void cuerpo_mas_largo_que_el_tamano_declarado_no_se_entrega_ni_se_consume_completo() throws Exception {
        final int declared = 1_000;
        final CountingStream body = new CountingStream(OVERSIZED_OBJECT_BYTES);
        final MinioFileStorageAdapter adapter = adapterServing(declared, body);

        assertThrows(FileStoragePort.StorageUnavailableException.class,
                () -> adapter.read(UUID.randomUUID().toString()));
        assertTrue(body.consumed() <= declared + READ_SLACK_BYTES,
                "Un cuerpo inconsistente con la metadata no debe leerse completo; bytes consumidos: "
                        + body.consumed());
    }

    @Test
    void objeto_dentro_del_presupuesto_se_restaura_completo() throws Exception {
        final CountingStream body = new CountingStream(MAX_ORIGINAL_BYTES);
        final MinioFileStorageAdapter adapter = adapterServing(MAX_ORIGINAL_BYTES, body);

        final FileStoragePort.StoredObject stored = adapter.read(UUID.randomUUID().toString());

        final byte[] expected = new byte[MAX_ORIGINAL_BYTES];
        java.util.Arrays.fill(expected, (byte) 'X');
        assertArrayEquals(expected, stored.content());
    }

    private static MinioFileStorageAdapter adapterServing(final long declaredSize, final InputStream body)
            throws Exception {
        final MinioClient client = mock(MinioClient.class);
        final StatObjectResponse stat = mock(StatObjectResponse.class);
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(stat);
        when(stat.size()).thenReturn(declaredSize);
        when(stat.contentType()).thenReturn("application/pdf");
        when(stat.userMetadata()).thenReturn(new Http.Headers(Map.of(
                "owner-subject", UUID.randomUUID().toString(),
                "original-filename", "soporte.pdf",
                "original-size", Long.toString(declaredSize),
                "compressed", "false",
                "compression-algorithm", "",
                "checksum-sha256", "0".repeat(64),
                "uploaded-at", "2026-10-08T12:00:00Z")));
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(
                new GetObjectResponse(Headers.of(), "test-private-bucket", "", "soportes/x", body));
        return new MinioFileStorageAdapter(client, "test-private-bucket");
    }

    /** Stream real (no mock) que entrega {@code total} bytes 'X' y cuenta lo consumido. */
    private static final class CountingStream extends InputStream {
        private final long total;
        private final AtomicLong consumed = new AtomicLong();

        private CountingStream(final long total) {
            this.total = total;
        }

        long consumed() {
            return consumed.get();
        }

        @Override
        public int read() {
            if (consumed.get() >= total) {
                return -1;
            }
            consumed.incrementAndGet();
            return 'X';
        }

        @Override
        public int read(final byte[] destination, final int offset, final int length) {
            final long remaining = total - consumed.get();
            if (remaining <= 0) {
                return -1;
            }
            final int n = (int) Math.min(remaining, length);
            java.util.Arrays.fill(destination, offset, offset + n, (byte) 'X');
            consumed.addAndGet(n);
            return n;
        }
    }
}
