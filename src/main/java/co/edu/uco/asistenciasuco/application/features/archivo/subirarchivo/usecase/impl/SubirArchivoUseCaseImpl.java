package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ContentSecurityValidator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ValidatedContent;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.SubirArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquesta: validacion de contenido -> analisis de malware (fail-closed) -> decision de
 * compresion -> checksum -> almacenamiento. Ver
 * docs/work-items/LB-004-stateless-serverless-readiness/CONTENT_SECURITY.md y
 * MINIO_STORAGE_CONTRACT.md para el contrato completo.
 */
public final class SubirArchivoUseCaseImpl implements SubirArchivoUseCase {

    private static final String ARCHIVO_URL_PREFIX = "/api/v1/archivos/";

    private final FileStoragePort fileStoragePort;
    private final MalwareScanPort malwareScanPort;

    public SubirArchivoUseCaseImpl(
            final FileStoragePort fileStoragePort,
            final MalwareScanPort malwareScanPort
    ) {
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort, "El puerto de salida FileStoragePort es obligatorio.");
        this.malwareScanPort = Objects.requireNonNull(malwareScanPort, "El puerto de salida MalwareScanPort es obligatorio.");
    }

    @Override
    public SubirArchivoResultadoEntity execute(final SubirArchivoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para subir un archivo es obligatorio.");
        }
        if (domain.ownerSubject() == null) {
            throw new CrosscuttingException("El propietario autenticado es obligatorio para subir un archivo.");
        }

        final ValidatedContent validated = ContentSecurityValidator.validate(
                domain.originalFilename(), domain.declaredContentType(), domain.content());

        scanForMalwareFailClosed(domain.content());

        final CompressionDecision compression = CompressionPolicy.evaluate(domain.content());
        final String checksum = ChecksumCalculator.sha256(domain.content());

        final UUID fileId = UUID.randomUUID();
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                domain.ownerSubject().toString(),
                validated.sanitizedFilename(),
                validated.verifiedContentType(),
                domain.content().length,
                compression.storedContent().length,
                compression.compressed(),
                compression.algorithm(),
                checksum,
                Instant.now()
        );

        fileStoragePort.store(new FileStoragePort.StoreObjectCommand(fileId.toString(), compression.storedContent(), metadata));

        return new SubirArchivoResultadoEntity(fileId, validated.sanitizedFilename(), ARCHIVO_URL_PREFIX + fileId, domain.content().length);
    }

    private void scanForMalwareFailClosed(final byte[] content) {
        final MalwareScanPort.ScanResult scanResult;
        try {
            scanResult = malwareScanPort.scan(content);
        } catch (final MalwareScanPort.MalwareScanException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el control de seguridad del archivo.", exception);
        }
        if (scanResult.isInfected()) {
            throw new ValidationException(
                    "El archivo fue rechazado: se detecto una firma de malware conocida.");
        }
    }
}
