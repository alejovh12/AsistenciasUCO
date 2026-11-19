package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SES-GEN-01 (migracion 57): las reglas nuevas del horario del grupo, de las horas semanales de la materia y de la duracion del periodo llegan
 * con su propio codigo y con el mensaje que resolvio la DB (horas, franja, tope, nombre del periodo y fecha de fin calculada): 409 si dependen del
 * estado de otra entidad (la materia, otros grupos, el periodo) y 400 si la entrada es invalida por si sola. SES_005 deja de caer en un 500 tecnico.
 */
class DbExceptionTranslatorSesGen01Test {

    private static <T extends RuntimeException> T fallo(final Class<T> tipo, final String dbCode, final String mensajeDb) {
        return assertThrows(tipo, () -> DbExceptionTranslator.throwIfFailed(
                false, mensajeDb, "DBCODE=" + dbCode + "|detalle tecnico", "corr", "operacionSesGen01"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "HOR_006;Las horas semanales del horario (3 h) no coinciden con las de la materia (4 h). Ajusta las franjas.",
            "HOR_009;El nuevo horario se cruza con otros grupos de 2 estudiante(s) inscrito(s) en la franja Lunes 08:00-10:00.",
            "ASI_010;La asignatura no está disponible para crear grupos.",
            "PER_010;El período académico 2025-1 no está vigente: ya terminó, está cerrado o no es de tu institución.",
            "SES_005;El horario institucional incluye una hora local inexistente por cambio de horario; no se generaron sesiones."
    })
    void las_reglas_que_dependen_de_otra_entidad_son_conflicto_409_con_el_mensaje_de_la_db(final String codigo, final String mensajeDb) {
        final ConflictException exception = fallo(ConflictException.class, codigo, mensajeDb);

        assertEquals(codigo, exception.getCode());
        assertEquals(mensajeDb, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "HOR_007;La franja Lunes 08:00-08:30 debe durar entre 60 y 240 minutos.",
            "HOR_008;La franja Lunes 05:00-07:00 debe estar dentro del horario permitido (06:00-22:00).",
            "ASI_013;Las horas semanales de la materia deben ser un entero entre 1 y 40.",
            "PER_011;El período debe durar 16 semanas: la fecha de fin debe ser 2027-05-25."
    })
    void las_entradas_invalidas_son_validacion_400_con_el_mensaje_de_la_db(final String codigo, final String mensajeDb) {
        final ValidationException exception = fallo(ValidationException.class, codigo, mensajeDb);

        assertEquals(codigo, exception.getCode());
        assertEquals(mensajeDb, exception.getMessage());
    }
}
