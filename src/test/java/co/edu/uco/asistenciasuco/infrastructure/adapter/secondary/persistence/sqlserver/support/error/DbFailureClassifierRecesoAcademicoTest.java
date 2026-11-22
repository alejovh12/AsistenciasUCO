package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** DEC-081 (migracion 68): codigos REC_* de la DB y su clasificacion HTTP (400 validacion, 404 inexistente, 409 negocio). */
class DbFailureClassifierRecesoAcademicoTest {

    @ParameterizedTest
    @CsvSource({"REC_003,VALIDATION", "REC_004,VALIDATION", "REC_008,VALIDATION", "REC_005,CONFLICT", "REC_006,CONFLICT",
            "REC_007,CONFLICT", "REC_010,CONFLICT", "REC_009,NOT_FOUND"})
    void cada_codigo_rec_se_clasifica_con_su_error_y_su_http(final String codigo, final String clase) {
        final var definicion = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "crearRecesoAcademico");

        assertEquals(MensajeUsuarioErrorCode.valueOf(codigo), definicion);
        assertEquals(codigo, definicion.code());
        final Class<? extends RuntimeException> esperada = switch (clase) {
            case "VALIDATION" -> ValidationException.class;
            case "CONFLICT" -> ConflictException.class;
            default -> ResourceNotFoundException.class;
        };
        final var texto = "Texto sustituido por la DB para " + codigo;
        final var excepcion = assertThrows(RuntimeException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, texto, "DBCODE=" + codigo + "|detalle", "c", "crearRecesoAcademico"));
        assertInstanceOf(esperada, excepcion);
    }

    @Test
    void el_mensaje_con_marcadores_sustituidos_por_la_db_se_conserva_en_el_conflicto() {
        final var mensaje = "Este día de receso ya canceló 4 clases y no se puede retirar ni cambiar.";

        final var excepcion = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=REC_010|detalle", "c", "retirarRecesoAcademico"));

        assertEquals("REC_010", excepcion.getErrorDefinition().orElseThrow().code());
        assertEquals(mensaje, excepcion.getMessage());
    }

    @Test
    void los_codigos_de_las_lecturas_se_traducen_por_dbcode() {
        assertInstanceOf(ResourceNotFoundException.class, DbExceptionTranslator.translateDbCode("REC_009", null).orElseThrow());
        assertInstanceOf(ResourceNotFoundException.class, DbExceptionTranslator.translateDbCode("PER_001", null).orElseThrow());
    }
}