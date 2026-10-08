package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RED causal LB-004B.2: SEC-002/SEC-003 — ownership tecnico fail-closed. Un actor no-owner
 * (estudiante ajeno, docente, coordinador, administrador) recibe 404 por igual: no hay
 * distincion de rol en Application mientras REVIEW_BINDING este bloqueado (ver
 * OWNERSHIP_DECISION.md §Addendum LB-004B.2).
 */
class DescargarArchivoUseCaseImplTest {

    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final DescargarArchivoUseCaseImpl useCase = new DescargarArchivoUseCaseImpl(fileStoragePort);

    @Test
    void dominio_nulo_es_rechazado() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    // SEC-002: owner valido -> exito, mismos bytes.
    @Test
    void owner_valido_recupera_bytes_y_contenido_original() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        final byte[] originalContent = "contenido pdf".getBytes();
        when(fileStoragePort.read(fileId.toString())).thenReturn(storedObjectFor(fileId, owner, originalContent, false, null));

        final DescargarArchivoResultadoEntity resultado = useCase.execute(new DescargarArchivoDomain(fileId, owner));

        assertArrayEquals(originalContent, resultado.content());
        assertEquals("application/pdf", resultado.contentType());
    }

    // SEC-003: actor autenticado no-owner -> 404, fail-closed, sin distinguir motivo.
    @Test
    void actor_no_owner_recibe_resource_not_found() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        final UUID otroActor = UUID.randomUUID();
        when(fileStoragePort.read(fileId.toString())).thenReturn(storedObjectFor(fileId, owner, "contenido".getBytes(), false, null));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, otroActor)));
    }

    @Test
    void fileId_inexistente_recibe_resource_not_found() {
        final UUID fileId = UUID.randomUUID();
        when(fileStoragePort.read(anyString())).thenThrow(new FileStoragePort.ObjectNotFoundException(fileId.toString()));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(fileId, UUID.randomUUID())));
    }

    @Test
    void contenido_comprimido_se_descomprime_de_forma_transparente() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        final byte[] original = "contenido original repetible repetible repetible repetible".getBytes();
        final co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision compressed =
                co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy.evaluate(original);
        when(fileStoragePort.read(fileId.toString()))
                .thenReturn(storedObjectFor(fileId, owner, compressed.storedContent(), compressed.compressed(), compressed.algorithm()));

        final DescargarArchivoResultadoEntity resultado = useCase.execute(new DescargarArchivoDomain(fileId, owner));

        assertArrayEquals(original, resultado.content());
    }

    // INTEGRITY-002: el checksum se verifica sobre los bytes originales, despues de descomprimir.
    @Test
    void checksum_incorrecto_falla_cerrado_y_no_retorna_contenido_corrupto() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        final byte[] corruptContent = "contenido alterado".getBytes();
        final FileStoragePort.StoredObject stored = storedObjectFor(
                fileId, owner, corruptContent, false, null, ChecksumCalculator.sha256("contenido original".getBytes()));
        when(fileStoragePort.read(fileId.toString())).thenReturn(stored);

        assertThrows(InternalApplicationException.class,
                () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
    }

    private FileStoragePort.StoredObject storedObjectFor(
            final UUID fileId,
            final UUID owner,
            final byte[] content,
            final boolean compressed,
            final String algorithm
    ) {
        return storedObjectFor(fileId, owner, content, compressed, algorithm, ChecksumCalculator.sha256(
                compressed ? co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy
                        .decompress(content, algorithm) : content));
    }

    private FileStoragePort.StoredObject storedObjectFor(
            final UUID fileId,
            final UUID owner,
            final byte[] content,
            final boolean compressed,
            final String algorithm,
            final String checksum
    ) {
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                owner.toString(),
                "soporte.pdf",
                "application/pdf",
                content.length,
                content.length,
                compressed,
                algorithm,
                checksum,
                Instant.now()
        );
        return new FileStoragePort.StoredObject(fileId.toString(), content, metadata);
    }
}


