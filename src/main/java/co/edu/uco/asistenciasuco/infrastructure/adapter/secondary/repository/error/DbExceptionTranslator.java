package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Traduce el contrato tecnico de SQL Server a excepciones semanticas de aplicacion.
 */
public final class DbExceptionTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbExceptionTranslator.class);
    private static final String SAFE_INTERNAL_MESSAGE = "No fue posible completar la operacion.";

    private static final Set<String> FORBIDDEN_CODES = Set.of(
            DbFailureClassifier.ERR_ESTUDIANTE_NO_PERTENECE_SESION
    );
    private static final Set<String> NOT_FOUND_CODES = Set.of(
            DbFailureClassifier.ERR_USUARIO_NO_EXISTE,
            DbFailureClassifier.ERR_ESTUDIANTE_NO_EXISTE,
            DbFailureClassifier.ERR_DOCENTE_NO_EXISTE,
            DbFailureClassifier.ERR_GRUPO_NO_EXISTE,
            DbFailureClassifier.ERR_TIPO_IDENTIFICACION_NO_EXISTE,
            DbFailureClassifier.ERR_SESION_NO_EXISTE
    );
    private static final Set<String> CONFLICT_CODES = Set.of(
            DbFailureClassifier.ERR_UNICIDAD_CORREO,
            DbFailureClassifier.ERR_UNICIDAD_DOCUMENTO,
            DbFailureClassifier.ERR_CUPO_SUPERADO,
            DbFailureClassifier.ERR_CRUCE_HORARIO_ESTUDIANTE,
            DbFailureClassifier.ERR_CRUCE_HORARIO_DOCENTE,
            DbFailureClassifier.ERR_MATRICULA_DUPLICADA,
            DbFailureClassifier.ERR_GRUPO_NO_HABILITADO,
            DbFailureClassifier.ERR_USUARIO_INACTIVO,
            DbFailureClassifier.ERR_ESTUDIANTE_INACTIVO,
            DbFailureClassifier.ERR_DOCENTE_INACTIVO,
            DbFailureClassifier.ERR_DOCENTE_YA_REGISTRADO
    );
    private static final Set<String> VALIDATION_CODES = Set.of(
            DbFailureClassifier.ERR_NOMBRE_PERSONA_INVALIDO
    );

    private static final Map<Set<String>, ExceptionFactory> EXCEPTION_BY_CODES = Map.of(
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

        final String code = DbFailureClassifier.classify(userMessage, technicalMessage, operation);
        logFailedResult(code, userMessage, technicalMessage, correlationId, operation);

        final String publicMessage = TextHelper.isNullOrBlank(userMessage) ? SAFE_INTERNAL_MESSAGE : userMessage;
        for (final Map.Entry<Set<String>, ExceptionFactory> entry : EXCEPTION_BY_CODES.entrySet()) {
            if (entry.getKey().contains(code)) {
                throw entry.getValue().create(code, publicMessage);
            }
        }

        throw new InternalApplicationException(DbFailureClassifier.ERR_DB_UNCLASSIFIED, SAFE_INTERNAL_MESSAGE);
    }

    private static void logFailedResult(
            final String internalCode,
            final String userMessage,
            final String technicalMessage,
            final String correlationId,
            final String operation
    ) {
        final String normalizedOperation = TextHelper.isNullOrBlank(operation) ? "unknown" : operation;
        if (DbFailureClassifier.ERR_DB_UNCLASSIFIED.equals(internalCode)) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                    normalizedOperation,
                    correlationId,
                    internalCode,
                    userMessage,
                    technicalMessage
            );
            return;
        }
        LOGGER.warn(
                "SQL operation rejected. operation={}, correlationId={}, dbCode={}, userMessage={}, technicalMessage={}",
                normalizedOperation,
                correlationId,
                internalCode,
                userMessage,
                technicalMessage
        );
    }

    @FunctionalInterface
    private interface ExceptionFactory {

        ApplicationException create(String code, String message);
    }
}
