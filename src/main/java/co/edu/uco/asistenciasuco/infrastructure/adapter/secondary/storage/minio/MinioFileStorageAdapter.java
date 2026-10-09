package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Unico componente que conoce el SDK de MinIO. Traduce excepciones tecnicas del SDK a
 * {@link FileStoragePort} y nunca registra access key, secret key ni payload de archivo.
 *
 * <p>El objectKey interno se deriva deterministicamente de {@code fileId}
 * ({@code soportes/<fileId>}); Application nunca ve esta representacion.</p>
 */
public final class MinioFileStorageAdapter implements FileStoragePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioFileStorageAdapter.class);
    private static final String OBJECT_KEY_PREFIX = "soportes/";
    /**
     * Presupuesto de lectura: un soporte valido nunca supera 5 MiB originales y su representacion
     * almacenada (comprimida solo si es estrictamente menor) tampoco (CONTENT_SECURITY de LB-004).
     */
    private static final long MAX_OBJECT_BYTES = 5L * 1024 * 1024;

    private static final String META_OWNER_SUBJECT = "owner-subject";
    private static final String META_ORIGINAL_FILENAME = "original-filename";
    private static final String META_ORIGINAL_SIZE = "original-size";
    private static final String META_COMPRESSED = "compressed";
    private static final String META_COMPRESSION_ALGORITHM = "compression-algorithm";
    private static final String META_CHECKSUM_SHA256 = "checksum-sha256";
    private static final String META_UPLOADED_AT = "uploaded-at";

    private final MinioClient minioClient;
    private final String bucket;

    public MinioFileStorageAdapter(final MinioClient minioClient, final String bucket) {
        this.minioClient = Objects.requireNonNull(minioClient, "MinioClient es obligatorio.");
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("El bucket de MinIO es obligatorio.");
        }
        this.bucket = bucket;
    }

    @Override
    public StoredObject store(final StoreObjectCommand command) {
        Objects.requireNonNull(command, "StoreObjectCommand es obligatorio.");
        final String objectKey = objectKeyOf(command.fileId());
        try (InputStream content = new ByteArrayInputStream(command.content())) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(content, (long) command.content().length, -1L)
                    .contentType(command.metadata().contentType())
                    .userMetadata(toUserMetadata(command.metadata()))
                    .build());
            LOGGER.info("storage upload success. fileId={}, size={}", command.fileId(), command.content().length);
            return new StoredObject(command.fileId(), command.content(), command.metadata());
        } catch (final Exception exception) {
            LOGGER.error("storage upload failure. fileId={}", command.fileId());
            throw new StorageUnavailableException("No fue posible almacenar el archivo en MinIO.", exception);
        }
    }

    @Override
    public StoredObject read(final String fileId) {
        Objects.requireNonNull(fileId, "fileId es obligatorio.");
        final String objectKey = objectKeyOf(fileId);
        try {
            final StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            final long declaredSize = stat.size();
            if (declaredSize < 0 || declaredSize > MAX_OBJECT_BYTES) {
                LOGGER.error("storage download rejected. fileId={}, reason=object_size_out_of_budget", fileId);
                throw new StorageUnavailableException("El objeto almacenado excede el tamano maximo permitido.", null);
            }
            final byte[] content = readBoundedBytes(objectKey, (int) declaredSize);
            LOGGER.info("storage download success. fileId={}, size={}", fileId, content.length);
            return new StoredObject(fileId, content, fromUserMetadata(stat));
        } catch (final StorageException exception) {
            throw exception;
        } catch (final ErrorResponseException exception) {
            if (isNotFound(exception)) {
                LOGGER.info("storage download not_found. fileId={}", fileId);
                throw new ObjectNotFoundException(fileId);
            }
            LOGGER.error("storage download failure. fileId={}", fileId);
            throw new StorageUnavailableException("No fue posible leer el archivo desde MinIO.", exception);
        } catch (final Exception exception) {
            LOGGER.error("storage download failure. fileId={}", fileId);
            throw new StorageUnavailableException("No fue posible leer el archivo desde MinIO.", exception);
        }
    }

    @Override
    public boolean exists(final String fileId) {
        Objects.requireNonNull(fileId, "fileId es obligatorio.");
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKeyOf(fileId))
                    .build());
            return true;
        } catch (final ErrorResponseException exception) {
            if (isNotFound(exception)) {
                return false;
            }
            throw new StorageUnavailableException("No fue posible verificar el archivo en MinIO.", exception);
        } catch (final Exception exception) {
            throw new StorageUnavailableException("No fue posible verificar el archivo en MinIO.", exception);
        }
    }

    @Override
    public void delete(final String fileId) {
        Objects.requireNonNull(fileId, "fileId es obligatorio.");
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKeyOf(fileId))
                    .build());
            LOGGER.info("storage delete success. fileId={}", fileId);
        } catch (final Exception exception) {
            LOGGER.error("storage delete failure. fileId={}", fileId);
            throw new StorageUnavailableException("No fue posible eliminar el archivo en MinIO.", exception);
        }
    }

    /**
     * Lee exactamente {@code declaredSize} bytes (ya validado contra {@link #MAX_OBJECT_BYTES}). Un
     * cuerpo mas corto o mas largo que la metadata del objeto es inconsistente y falla cerrado sin
     * consumir el resto del stream.
     */
    private byte[] readBoundedBytes(final String objectKey, final int declaredSize) throws Exception {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build())) {
            final byte[] content = stream.readNBytes(declaredSize);
            if (content.length != declaredSize || stream.read() != -1) {
                throw new StorageUnavailableException(
                        "El contenido leido desde MinIO no coincide con el tamano del objeto.", null);
            }
            return content;
        } catch (final IOException exception) {
            throw new StorageUnavailableException("No fue posible leer el contenido del archivo desde MinIO.", exception);
        }
    }

    private static boolean isNotFound(final ErrorResponseException exception) {
        final String code = exception.errorResponse() == null ? null : exception.errorResponse().code();
        return "NoSuchKey".equals(code) || "NoSuchObject".equals(code);
    }

    private static String objectKeyOf(final String fileId) {
        return OBJECT_KEY_PREFIX + fileId;
    }

    private static Map<String, String> toUserMetadata(final StoredObjectMetadata metadata) {
        final Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put(META_OWNER_SUBJECT, metadata.ownerSubject());
        userMetadata.put(META_ORIGINAL_FILENAME, metadata.originalFilename());
        userMetadata.put(META_ORIGINAL_SIZE, Long.toString(metadata.originalSize()));
        userMetadata.put(META_COMPRESSED, Boolean.toString(metadata.compressed()));
        userMetadata.put(META_COMPRESSION_ALGORITHM, metadata.compressionAlgorithm() == null ? "" : metadata.compressionAlgorithm());
        userMetadata.put(META_CHECKSUM_SHA256, metadata.checksumSha256());
        userMetadata.put(META_UPLOADED_AT, metadata.uploadedAt().toString());
        return userMetadata;
    }

    private static StoredObjectMetadata fromUserMetadata(final StatObjectResponse stat) {
        final io.minio.Http.Headers userMetadata = stat.userMetadata();
        final String originalSize = firstOrDefault(userMetadata, META_ORIGINAL_SIZE, "0");
        final String compressionAlgorithm = firstOrDefault(userMetadata, META_COMPRESSION_ALGORITHM, "");
        return new StoredObjectMetadata(
                userMetadata.getFirst(META_OWNER_SUBJECT),
                userMetadata.getFirst(META_ORIGINAL_FILENAME),
                stat.contentType(),
                Long.parseLong(originalSize),
                stat.size(),
                Boolean.parseBoolean(userMetadata.getFirst(META_COMPRESSED)),
                compressionAlgorithm.isBlank() ? null : compressionAlgorithm,
                userMetadata.getFirst(META_CHECKSUM_SHA256),
                Instant.parse(userMetadata.getFirst(META_UPLOADED_AT))
        );
    }

    private static String firstOrDefault(final io.minio.Http.Headers headers, final String name, final String defaultValue) {
        final String value = headers.getFirst(name);
        return value == null ? defaultValue : value;
    }
}
