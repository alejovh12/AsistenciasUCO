package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba candidata RED, QUALITY-PR15 F04.
 * Mock del SDK (no demuestra comportamiento de MinIO real).
 * Verifica que una lectura inesperadamente grande NO se consuma completa en memoria.
 */
class MinioReadBudgetTest {

    private static final int MAX_ORIGINAL_BYTES = 5 * 1024 * 1024;
    private static final int OVERSIZED_OBJECT_BYTES = MAX_ORIGINAL_BYTES + 256 * 1024;

    @Test
    void lectura_del_objeto_sobre_el_presupuesto_aborta_sin_consumirlo_completo() throws Exception {
        final MinioClient client = mock(MinioClient.class);
        final StatObjectResponse stat = mock(StatObjectResponse.class);
        final GetObjectResponse response = mock(GetObjectResponse.class);
        final AtomicInteger consumed = new AtomicInteger();

        when(client.statObject(any(StatObjectArgs.class))).thenReturn(stat);
        when(stat.size()).thenReturn((long) OVERSIZED_OBJECT_BYTES);
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(response);
        when(response.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(invocation -> {
            final int remaining = OVERSIZED_OBJECT_BYTES - consumed.get();
            if (remaining <= 0) {
                return -1;
            }
            final byte[] destination = invocation.getArgument(0);
            final int offset = invocation.getArgument(1);
            final int requested = invocation.getArgument(2);
            final int n = Math.min(remaining, requested);
            Arrays.fill(destination, offset, offset + n, (byte) 'X');
            consumed.addAndGet(n);
            return n;
        });

        final MinioFileStorageAdapter adapter = new MinioFileStorageAdapter(client, "test-private-bucket");
        assertThrows(FileStoragePort.StorageUnavailableException.class,
                () -> adapter.read(UUID.randomUUID().toString()));
        assertTrue(consumed.get() <= MAX_ORIGINAL_BYTES + 8_192,
                "No debe leer el objeto entero antes de rechazarlo; bytes consumidos: " + consumed.get());
    }
}
