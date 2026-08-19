package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.error;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Clasifica mensajes devueltos por SQL Server a codigos internos estables.
 */
final class DbFailureClassifier {

    private static final String OPERATION_ASIGNAR_DOCENTE = "asignarDocenteAGrupo";
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";

    private DbFailureClassifier() {
        throw new IllegalStateException("No es permitido instanciar un clasificador de errores de persistencia.");
    }

    static ErrorCode classify(final String userMessage, final String technicalMessage, final String operation) {
        final String message = normalize(userMessage + " " + technicalMessage);
        final String normalizedOperation = normalize(operation);

        if (TextHelper.isNullOrBlank(message)) {
            return ErrorCode.ERR_DB_UNCLASSIFIED;
        }
        if (containsAny(message, "nombre", "nombres", "apellido", "apellidos")
                && containsAny(message, "caracteres no permitidos", "formato invalido")) {
            return ErrorCode.ERR_NOMBRE_PERSONA_INVALIDO;
        }
        if (contains(message, "usuario") && contains(message, "registrado como docente")) {
            return ErrorCode.ERR_DOCENTE_YA_REGISTRADO;
        }
        if (contains(message, "estudiante") && contains(message, "inactivo")) {
            return ErrorCode.ERR_ESTUDIANTE_INACTIVO;
        }
        if (contains(message, "docente") && contains(message, "inactivo")) {
            return ErrorCode.ERR_DOCENTE_INACTIVO;
        }
        if (contains(message, "usuario") && contains(message, "inactivo")) {
            return ErrorCode.ERR_USUARIO_INACTIVO;
        }
        if (contains(message, "matricula") && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "inscrit")) {
            return ErrorCode.ERR_MATRICULA_DUPLICADA;
        }
        if (containsAny(message, "tamaoo moximo", "tamano maximo")) {
            return ErrorCode.ERR_CUPO_SUPERADO;
        }
        if (containsAny(message, "cupo", "cupos")
                && containsAny(message, "superad", "sin", "no hay", "agotad", "maxim", "moximo", "capacidad", "lleno", "llena")) {
            return ErrorCode.ERR_CUPO_SUPERADO;
        }
        if (isScheduleConflict(message)) {
            if (contains(message, "estudiante") || contains(normalizedOperation, OPERATION_REGISTRAR_ESTUDIANTE.toLowerCase(Locale.ROOT))) {
                return ErrorCode.ERR_CRUCE_HORARIO_ESTUDIANTE;
            }
            if (contains(message, "docente") || contains(normalizedOperation, OPERATION_ASIGNAR_DOCENTE.toLowerCase(Locale.ROOT))) {
                return ErrorCode.ERR_CRUCE_HORARIO_DOCENTE;
            }
        }
        if (containsAny(message, "correo", "email", "e-mail") && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "unico")) {
            return ErrorCode.ERR_UNICIDAD_CORREO;
        }
        if (containsAny(message, "documento", "identificacion", "numero de identificacion")
                && containsAny(message, "duplicad", "ya existe", "ya se encuentra", "registrad", "unico")) {
            return ErrorCode.ERR_UNICIDAD_DOCUMENTO;
        }
        if (contains(message, "grupo") && containsAny(
                message,
                "no habilitado",
                "deshabilitado",
                "inhabilitado",
                "no esta habilitado",
                "no se encuentra habilitado"
        )) {
            return ErrorCode.ERR_GRUPO_NO_HABILITADO;
        }
        if (containsAny(
                message,
                "tipo identificacion",
                "tipo de identificacion",
                "tipo identificaci?n",
                "tipo de identificaci?n",
                "tipoididentificacion",
                "uv_tipo_identificacion"
        )
                && containsAny(message, "no existe", "no encontrado", "no encontrada", "inexistente")) {
            return ErrorCode.ERR_TIPO_IDENTIFICACION_NO_EXISTE;
        }
        if (doesNotExist(message, "grupo")) {
            return ErrorCode.ERR_GRUPO_NO_EXISTE;
        }
        if (doesNotExist(message, "estudiante")) {
            return ErrorCode.ERR_ESTUDIANTE_NO_EXISTE;
        }
        if (doesNotExist(message, "usuario")) {
            return ErrorCode.ERR_USUARIO_NO_EXISTE;
        }
        if (doesNotExist(message, "docente")) {
            return ErrorCode.ERR_DOCENTE_NO_EXISTE;
        }
        if (doesNotExist(message, "sesion")) {
            return ErrorCode.ERR_SESION_NO_EXISTE;
        }
        if (contains(message, "estudiante") && contains(message, "sesion")
                && containsAny(message, "no pertenece", "no esta asociado", "no se encuentra asociado")) {
            return ErrorCode.ERR_ESTUDIANTE_NO_PERTENECE_SESION;
        }

        return ErrorCode.ERR_DB_UNCLASSIFIED;
    }

    private static boolean doesNotExist(final String message, final String resource) {
        return contains(message, resource)
                && containsAny(message, "no existe", "no encontrado", "no encontrada", "inexistente");
    }

    private static boolean isScheduleConflict(final String message) {
        return containsAny(message, "cruce", "conflicto", "solap")
                && containsAny(message, "horario", "hora", "sesion");
    }

    private static boolean containsAny(final String value, final String... expectedValues) {
        for (final String expectedValue : expectedValues) {
            if (contains(value, expectedValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(final String value, final String expectedValue) {
        return value.contains(normalize(expectedValue));
    }

    private static String normalize(final String value) {
        if (TextHelper.isNullOrBlank(value)) {
            return "";
        }
        final String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }
}
