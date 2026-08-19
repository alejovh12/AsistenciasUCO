package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Traduce el contrato tecnico de SQL Server a excepciones semanticas de aplicacion.
 */
public final class DbExceptionTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbExceptionTranslator.class);
    private static final Set<ErrorCode> FORBIDDEN_CODES = Set.of(
            ErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION
    );
    private static final Set<ErrorCode> NOT_FOUND_CODES = Set.of(
            ErrorCode.ERR_USUARIO_NO_EXISTE,
            ErrorCode.ERR_ESTUDIANTE_NO_EXISTE,
            ErrorCode.ERR_DOCENTE_NO_EXISTE,
            ErrorCode.ERR_GRUPO_NO_EXISTE,
            ErrorCode.ERR_TIPO_IDENTIFICACION_NO_EXISTE,
            ErrorCode.ERR_SESION_NO_EXISTE
    );
    private static final Set<ErrorCode> CONFLICT_CODES = Set.of(
            ErrorCode.ERR_UNICIDAD_CORREO,
            ErrorCode.ERR_UNICIDAD_DOCUMENTO,
            ErrorCode.ERR_CUPO_SUPERADO,
            ErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE,
            ErrorCode.ERR_CRUCE_HORARIO_DOCENTE,
            ErrorCode.ERR_MATRICULA_DUPLICADA,
            ErrorCode.ERR_GRUPO_NO_HABILITADO,
            ErrorCode.ERR_USUARIO_INACTIVO,
            ErrorCode.ERR_ESTUDIANTE_INACTIVO,
            ErrorCode.ERR_DOCENTE_INACTIVO,
            ErrorCode.ERR_DOCENTE_YA_REGISTRADO
    );
    private static final Set<ErrorCode> VALIDATION_CODES = Set.of(
            ErrorCode.ERR_NOMBRE_PERSONA_INVALIDO
    );

    private static final Map<Set<ErrorCode>, ExceptionFactory> EXCEPTION_BY_CODES = Map.of(
            FORBIDDEN_CODES, ForbiddenException::new,
            NOT_FOUND_CODES, ResourceNotFoundException::new,
            CONFLICT_CODES, ConflictException::new,
            VALIDATION_CODES, ValidationException::new
    );

    private DbExceptionTranslator() {
        throw new IllegalStateException("No es permitido instanciar un traductor de errores de persistencia.");
    }

    public static void throwIfFailed(
            final boolean successful,
            final String userMessage,
            final String technicalMessage,
            final String correlationId,
            final String operation
    ) {
        if (successful) {
            return;
        }

        final ErrorCode code = DbFailureClassifier.classify(userMessage, technicalMessage, operation);
        logFailedResult(code, userMessage, technicalMessage, correlationId, operation);

        for (final Map.Entry<Set<ErrorCode>, ExceptionFactory> entry : EXCEPTION_BY_CODES.entrySet()) {
            if (entry.getKey().contains(code)) {
                throw entry.getValue().create(code);
            }
        }

        throw new InternalApplicationException(ErrorCode.ERR_DB_UNCLASSIFIED);
    }

    private static void logFailedResult(
            final ErrorCode internalCode,
            final String userMessage,
            final String technicalMessage,
            final String correlationId,
            final String operation
    ) {
        final String normalizedOperation = TextHelper.isNullOrBlank(operation) ? "unknown" : operation;
        if (ErrorCode.ERR_DB_UNCLASSIFIED.equals(internalCode)) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                    normalizedOperation,
                    SensitiveDataSanitizer.sanitizeForLog(correlationId, 80),
                    internalCode.code(),
                    SensitiveDataSanitizer.sanitizeForLog(userMessage),
                    SensitiveDataSanitizer.sanitizeForLog(technicalMessage)
            );
            return;
        }
        LOGGER.warn(
                "SQL operation rejected. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                normalizedOperation,
                SensitiveDataSanitizer.sanitizeForLog(correlationId, 80),
                internalCode.code(),
                SensitiveDataSanitizer.sanitizeForLog(userMessage),
                SensitiveDataSanitizer.sanitizeForLog(technicalMessage)
        );
    }

    @FunctionalInterface
    private interface ExceptionFactory {

        ApplicationException create(ErrorCode code);
    }
}
