package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.CatalogoErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU074 (migracion 58): ASI_006 (asignatura compartida con programas que el coordinador no coordina), ASI_009 (inactivar con
 * grupos en el periodo vigente o futuro) y ASI_010 (asignatura inactiva al crear un grupo o asociarla a un plan) son conflictos
 * de negocio (409) con su propio codigo, para que el usuario reciba el texto del catalogo y no el generico de CONFLICT.
 */
@DisplayName("HU074 - rechazos de la asignatura activa o inactiva")
class DbExceptionTranslatorAsignaturaEstadoTest {

    /** Codigo de rechazo y operacion del SP que lo emite. */
    static Stream<Arguments> rechazosPorOperacion() {
        return Stream.of(
                Arguments.of("ASI_006", "toggleEstadoAsignatura"),
                Arguments.of("ASI_009", "toggleEstadoAsignatura"),
                Arguments.of("ASI_010", "crearGrupo"),
                Arguments.of("ASI_010", "asociarAsignaturaPlan"));
    }

    @ParameterizedTest
    @MethodSource("rechazosPorOperacion")
    void el_clasificador_los_publica_como_conflicto_con_su_codigo(final String codigo, final String operacion) {
        final var definicion = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", operacion);

        assertEquals(CatalogoErrorCode.valueOf(codigo), definicion);
        assertEquals(codigo, definicion.messageCode());
        assertEquals(ErrorKind.CONFLICT, definicion.kind());
    }

    @ParameterizedTest
    @MethodSource("rechazosPorOperacion")
    void el_rechazo_del_sp_sale_como_conflict_exception_con_el_codigo_del_catalogo(final String codigo, final String operacion) {
        final ConflictException exception = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "Texto del catalogo.", "DBCODE=" + codigo + "|detalle tecnico", "corr", operacion));

        assertEquals(codigo, exception.getCode());
        final String mensajeEsperado = "ASI_010".equals(codigo)
                ? "Texto del catalogo."
                : CatalogoErrorCode.valueOf(codigo).defaultMessage();
        assertEquals(mensajeEsperado, exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"ASI_006", "ASI_009", "ASI_010"})
    void un_sp_de_consulta_que_lanza_el_codigo_tambien_responde_409(final String codigo) {
        final RuntimeException exception = DbExceptionTranslator.translateDbCode(codigo, "consultarOfertaCreacionGrupo").orElseThrow();

        assertEquals(ConflictException.class, exception.getClass());
        assertEquals(codigo, ((ConflictException) exception).getCode());
    }

    // HU074: el codigo de exito del SP (ASI_011 / ASI_012) se lee del mismo canal tecnico DBCODE=<codigo>|<detalle>.
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", delimiter = ';', value = {
            "DBCODE=ASI_011|HU074: Asignatura.estado pasa de 0 a 1. Correlacion: x;ASI_011",
            "DBCODE=ASI_012|;ASI_012",
            "Mensaje libre sin canal;NULL",
            "NULL;NULL"
    })
    void dbCodeOf_extrae_el_codigo_del_canal_tecnico(final String mensajeTecnico, final String esperado) {
        assertEquals(esperado, DbExceptionTranslator.dbCodeOf(mensajeTecnico).orElse(null));
    }
}
