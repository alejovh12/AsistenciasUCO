package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * QUALITY-PR15 F06/F08: ownership DENY_BY_DEFAULT (OWNERSHIP_DECISION / PROFESSOR_DECISION
 * addendum 7) y errores tecnicos no enmascarados como 404.
 */
class DescargarArchivoFailClosedTest {

    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final DescargarArchivoUseCaseImpl useCase = new DescargarArchivoUseCaseImpl(storage);

    @Test
    void objeto_sin_propietario_registrado_se_niega_como_inexistente() {
        final UUID fileId = UUID.randomUUID();
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, null, false, null, "%PDF-".getBytes()));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(fileId, UUID.randomUUID())));
    }

    @Test
    void actor_ajeno_no_dispara_descompresion_ni_verificacion_del_contenido() {
        final UUID fileId = UUID.randomUUID();
        final byte[] notDeflate = {1, 2, 3, 4};
        when(storage.read(fileId.toString()))
                .thenReturn(stored(fileId, UUID.randomUUID().toString(), true, "DEFLATE", notDeflate));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(fileId, UUID.randomUUID())));
    }

    @Test
    void identificadores_ausentes_no_consultan_storage() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(null, UUID.randomUUID())));
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(UUID.randomUUID(), null)));
        verifyNoInteractions(storage);
    }

    @Test
    void storage_no_disponible_no_se_disfraza_de_archivo_inexistente() {
        final FileStoragePort.StorageUnavailableException unavailable =
                new FileStoragePort.StorageUnavailableException("minio down", new RuntimeException());
        when(storage.read(anyString())).thenThrow(unavailable);

        final FileStoragePort.StorageUnavailableException thrown = assertThrows(
                FileStoragePort.StorageUnavailableException.class,
                () -> useCase.execute(new DescargarArchivoDomain(UUID.randomUUID(), UUID.randomUUID())));
        assertSame(unavailable, thrown);
    }

    private static FileStoragePort.StoredObject stored(final UUID fileId, final String owner, final boolean compressed,
                                                       final String algorithm, final byte[] content) {
        return new FileStoragePort.StoredObject(fileId.toString(), content, new FileStoragePort.StoredObjectMetadata(
                owner, "soporte.pdf", "application/pdf", content.length, content.length, compressed, algorithm,
                "0".repeat(64), Instant.parse("2026-10-08T12:00:00Z")));
    }
}
