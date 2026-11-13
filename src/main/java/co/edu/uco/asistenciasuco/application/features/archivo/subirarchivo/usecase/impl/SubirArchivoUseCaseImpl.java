package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.internal.ServiceUnavailableException;
import co.edu.uco.asistenciasuco.application.features.archivo.exception.ArchivoErrorCode;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ContentSecurityValidator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ValidatedContent;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.SubirArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquesta: validacion de contenido -> analisis de malware (fail-closed) -> decision de
 * compresion -> checksum -> almacenamiento -> registro durable del borrador (DRAFT) en SQL. Ver
 * docs/work-items/LB-004-stateless-serverless-readiness/CONTENT_SECURITY.md y
 * MINIO_STORAGE_CONTRACT.md para el contrato completo.
 *
 * <p>MinIO no participa en la transaccion SQL. Si SQL rechaza el registro de forma definitiva (no se
 * escribio nada) el objeto recien almacenado se elimina como compensacion best-effort. Si el
 * resultado es desconocido (transporte/timeout) se reintenta una vez con la misma metadata
 * (idempotente) y, si persiste, NO se borran los bytes: puede existir la fila y la reconciliacion
 * posterior decide.</p>
 */
public final class SubirArchivoUseCaseImpl implements SubirArchivoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubirArchivoUseCaseImpl.class);
    private static final String ARCHIVO_URL_PREFIX = "/api/v1/archivos/";

    private final FileStoragePort fileStoragePort;
    private final MalwareScanPort malwareScanPort;
    private final SoporteArchivoMetadataPort soporteArchivoMetadataPort;
    /** PAR-05: {@code null} = sin catalogo (pruebas unitarias); se aplica el respaldo de 5 MiB. */
    private final ParameterCatalogPort parametros;

    static final String GRUPO_PARAMETRO_TAMANIO = "ARCHIVO";
    static final String CLAVE_PARAMETRO_TAMANIO = "TAMANIO_MAXIMO_BYTES";

    public SubirArchivoUseCaseImpl(
            final FileStoragePort fileStoragePort,
            final MalwareScanPort malwareScanPort,
            final SoporteArchivoMetadataPort soporteArchivoMetadataPort
    ) {
        this(fileStoragePort, malwareScanPort, soporteArchivoMetadataPort, null);
    }

    public SubirArchivoUseCaseImpl(
            final FileStoragePort fileStoragePort,
            final MalwareScanPort malwareScanPort,
            final SoporteArchivoMetadataPort soporteArchivoMetadataPort,
            final ParameterCatalogPort parametros
    ) {
        this.parametros = parametros;
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort, "El puerto de salida FileStoragePort es obligatorio.");
        this.malwareScanPort = Objects.requireNonNull(malwareScanPort, "El puerto de salida MalwareScanPort es obligatorio.");
        this.soporteArchivoMetadataPort = Objects.requireNonNull(
                soporteArchivoMetadataPort, "El puerto de salida SoporteArchivoMetadataPort es obligatorio.");
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
                domain.originalFilename(), domain.declaredContentType(), domain.content(), tamanioMaximoBytes());

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

        registrarBorradorDurable(new SoporteArchivoMetadataPort.RegistrarSoporteBorrador(
                fileId,
                domain.ownerSubject(),
                validated.sanitizedFilename(),
                validated.verifiedContentType(),
                domain.content().length,
                checksum
        ));

        return new SubirArchivoResultadoEntity(fileId, validated.sanitizedFilename(), ARCHIVO_URL_PREFIX + fileId, domain.content().length);
    }

    /**
     * PAR-05: tamano maximo vigente desde {@code ARCHIVO/TAMANIO_MAXIMO_BYTES} (el mismo valor que publica
     * {@code GET /api/v1/configuracion/reclamos}). Sin catalogo, sin valor o con un valor no numerico se usa el respaldo
     * {@link ContentSecurityValidator#MAX_FILE_SIZE_BYTES}; el validador acota el valor al techo tecnico.
     */
    long tamanioMaximoBytes() {
        if (parametros == null) {
            return ContentSecurityValidator.MAX_FILE_SIZE_BYTES;
        }
        try {
            return parametros.getParameter(GRUPO_PARAMETRO_TAMANIO, CLAVE_PARAMETRO_TAMANIO)
                    .map(String::strip)
                    .map(Long::parseLong)
                    .orElse(ContentSecurityValidator.MAX_FILE_SIZE_BYTES);
        } catch (final RuntimeException _) {
            LOGGER.warn("parametro {}/{} no disponible o invalido; se aplica el respaldo de {} bytes",
                    GRUPO_PARAMETRO_TAMANIO, CLAVE_PARAMETRO_TAMANIO, ContentSecurityValidator.MAX_FILE_SIZE_BYTES);
            return ContentSecurityValidator.MAX_FILE_SIZE_BYTES;
        }
    }

    private void registrarBorradorDurable(final SoporteArchivoMetadataPort.RegistrarSoporteBorrador comando) {
        try {
            soporteArchivoMetadataPort.registrarBorrador(comando);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataRechazadaException rechazo) {
            // Rechazo definitivo: SQL no escribio nada, el objeto es un huerfano seguro de compensar.
            compensarObjeto(comando.fileId());
            throw new InternalApplicationException("No fue posible registrar el soporte adjunto.", rechazo);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException desconocido) {
            reintentarRegistroUnaVez(comando, desconocido);
        }
    }

    private void reintentarRegistroUnaVez(
            final SoporteArchivoMetadataPort.RegistrarSoporteBorrador comando,
            final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException primerFallo
    ) {
        try {
            // Mismo fileId, metadata y propietario: el registro es idempotente en SQL.
            soporteArchivoMetadataPort.registrarBorrador(comando);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataRechazadaException rechazo) {
            compensarObjeto(comando.fileId());
            throw new InternalApplicationException("No fue posible registrar el soporte adjunto.", rechazo);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException persistente) {
            // Resultado desconocido: NO se borran los bytes (puede existir la fila DRAFT).
            if (persistente != primerFallo) {
                persistente.addSuppressed(primerFallo);
            }
            throw new InternalApplicationException("No fue posible confirmar el registro del soporte adjunto.", persistente);
        }
    }

    private void compensarObjeto(final UUID fileId) {
        try {
            fileStoragePort.delete(fileId.toString());
        } catch (final RuntimeException exception) {
            // Best-effort: si falla queda un huerfano tecnico sin fila, atendido por la reconciliacion.
            LOGGER.warn("Compensacion de objeto de soporte no completada. fileId={}", fileId);
        }
    }

    private void scanForMalwareFailClosed(final byte[] content) {
        final MalwareScanPort.ScanResult scanResult;
        try {
            scanResult = malwareScanPort.scan(content);
        } catch (final MalwareScanPort.MalwareScanException exception) {
            // VAL H3: antivirus caido, timeout, respuesta invalida o breaker abierto. Falla cerrado (no se almacena) y responde 503
            // ERR_ANTIVIRUS_NO_DISPONIBLE en lugar de 500.
            throw new ServiceUnavailableException(ArchivoErrorCode.ERR_ANTIVIRUS_NO_DISPONIBLE, exception);
        }
        if (scanResult.isInfected()) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_028);
        }
    }
}
