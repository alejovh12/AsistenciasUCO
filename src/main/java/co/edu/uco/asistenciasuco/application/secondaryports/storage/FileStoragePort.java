package co.edu.uco.asistenciasuco.application.secondaryports.storage;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Puerto secundario neutral para almacenar y recuperar los bytes de un soporte de revision.
 *
 * <p>Agnostico a la tecnologia de storage (MinIO, Azure Blob, filesystem, etc.). No menciona
 * {@code MinioClient}, {@code bucket}, {@code S3}, {@code Blob}, filesystem ni presigned URL:
 * Application solo conoce {@code fileId} (UUID opaco) y metadata tecnica neutral.</p>
 */
public interface FileStoragePort {

    /**
     * Almacena un objeto nuevo. El {@code fileId} ya viene generado por Application.
     */
    StoredObject store(StoreObjectCommand command);

    /**
     * Recupera bytes y metadata de un objeto existente.
     *
     * @throws ObjectNotFoundException si no existe ningun objeto con ese {@code fileId}.
     */
    StoredObject read(String fileId);

    /**
     * Verifica existencia sin transferir bytes.
     */
    boolean exists(String fileId);

    /**
     * Elimina un objeto. Uso interno de reconciliacion (huerfanos/reemplazo); no expuesto como
     * endpoint HTTP publico.
     */
    void delete(String fileId);

    record StoreObjectCommand(String fileId, byte[] content, StoredObjectMetadata metadata) {

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof StoreObjectCommand that
                    && Objects.equals(fileId, that.fileId)
                    && Arrays.equals(content, that.content)
                    && Objects.equals(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(fileId, metadata) + Arrays.hashCode(content);
        }

        /** No expone el payload del archivo; solo su longitud. */
        @Override
        public String toString() {
            return "StoreObjectCommand[fileId=" + fileId + ", content=" + describe(content)
                    + ", metadata=" + metadata + "]";
        }
    }

    record StoredObject(String fileId, byte[] content, StoredObjectMetadata metadata) {

        @Override
        public boolean equals(final Object other) {
            if (this == other) {
                return true;
            }
            return other instanceof StoredObject that
                    && Objects.equals(fileId, that.fileId)
                    && Arrays.equals(content, that.content)
                    && Objects.equals(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(fileId, metadata) + Arrays.hashCode(content);
        }

        /** No expone el payload del archivo; solo su longitud. */
        @Override
        public String toString() {
            return "StoredObject[fileId=" + fileId + ", content=" + describe(content) + ", metadata=" + metadata + "]";
        }
    }

    private static String describe(final byte[] content) {
        return content == null ? "null" : content.length + " bytes";
    }

    record StoredObjectMetadata(
            String ownerSubject,
            String originalFilename,
            String contentType,
            long originalSize,
            long storedSize,
            boolean compressed,
            String compressionAlgorithm,
            String checksumSha256,
            Instant uploadedAt
    ) {
    }

    /**
     * Error tecnico base del storage (transporte, configuracion, respuesta inesperada del provider).
     */
    class StorageException extends RuntimeException {
        public StorageException(final String message) {
            super(message);
        }

        public StorageException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * El provider de storage no esta disponible (timeout, conexion rechazada, error 5xx del provider).
     */
    class StorageUnavailableException extends StorageException {
        public StorageUnavailableException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * No existe ningun objeto con el {@code fileId} solicitado.
     */
    class ObjectNotFoundException extends StorageException {
        public ObjectNotFoundException(final String fileId) {
            super("No existe un objeto de storage para el identificador solicitado.");
        }
    }
}
