package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.DescargarArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * Ownership tecnico fail-closed: el propietario del objeto (resuelto de la identidad JWT en el
 * momento de la subida) debe coincidir exactamente con el solicitante. Cualquier otro actor,
 * incluido docente/coordinador/administrador mientras REVIEW_BINDING este bloqueado, recibe 404
 * (nunca 403) para no revelar existencia. Ver OWNERSHIP_DECISION.md.
 */
public final class DescargarArchivoUseCaseImpl implements DescargarArchivoUseCase {

    private final FileStoragePort fileStoragePort;

    public DescargarArchivoUseCaseImpl(final FileStoragePort fileStoragePort) {
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort, "El puerto de salida FileStoragePort es obligatorio.");
    }

    @Override
    public DescargarArchivoResultadoEntity execute(final DescargarArchivoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para descargar un archivo es obligatorio.");
        }
        if (domain.fileId() == null || domain.requesterSubject() == null) {
            throw new ResourceNotFoundException("El archivo solicitado no existe.");
        }

        final FileStoragePort.StoredObject stored;
        try {
            stored = fileStoragePort.read(domain.fileId().toString());
        } catch (final FileStoragePort.ObjectNotFoundException exception) {
            throw new ResourceNotFoundException("El archivo solicitado no existe.");
        }

        // DENY_BY_DEFAULT: metadata ausente o sin propietario nunca concede acceso.
        if (stored.metadata() == null
                || !domain.requesterSubject().toString().equals(stored.metadata().ownerSubject())) {
            throw new ResourceNotFoundException("El archivo solicitado no existe.");
        }

        final byte[] originalContent = stored.metadata().compressed()
                ? CompressionPolicy.decompress(stored.content(), stored.metadata().compressionAlgorithm())
                : stored.content();

        final String actualChecksum = ChecksumCalculator.sha256(originalContent);
        if (!actualChecksum.equals(stored.metadata().checksumSha256())) {
            throw new InternalApplicationException(
                    "La verificacion de integridad del archivo almacenado no fue satisfactoria.");
        }

        return new DescargarArchivoResultadoEntity(
                originalContent,
                stored.metadata().contentType(),
                stored.metadata().originalFilename(),
                originalContent.length
        );
    }
}
