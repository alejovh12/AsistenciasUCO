package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 / DEC-064: los codigos de la cancelacion por el coordinador se exponen tal cual: {@code SES_018} (no manual) es 409 y
 * {@code SES_019}/{@code SES_021} (motivo y observacion) son 400. DEC-065 (2): {@code SES_023} (sesion pendiente), {@code SES_024}
 * (solicitud ya resuelta) y {@code SES_025} (sin solicitud) son 409.
 */
class DbFailureClassifierSesionManualTest {

    @ParameterizedTest
    @CsvSource({
            "SES_018, SES_018, CONFLICT",
            "SES_019, SES_019, VALIDATION",
            "SES_021, SES_021, VALIDATION",
            "SES_023, SES_023, CONFLICT",
            "SES_024, SES_024, CONFLICT",
            "SES_025, SES_025, CONFLICT"
    })
    void los_codigos_de_la_cancelacion_por_el_coordinador_se_clasifican_con_su_tipo(
            final String codigoDb, final String codigoPublico, final ErrorKind tipo) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", "cancelarSesionCoordinador");
        assertEquals(codigoPublico, clasificado.code());
        assertEquals(tipo, clasificado.kind());
    }

    @Test
    void se_traducen_a_las_excepciones_http_correspondientes() {
        assertEquals("SES_018", assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "m", "DBCODE=SES_018|no manual", "corr", "cancelarSesionCoordinador")).getCode());
        assertEquals("SES_019", assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "m", "DBCODE=SES_019|motivo", "corr", "cancelarSesionCoordinador")).getCode());
        assertEquals("SES_021", assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, "m", "DBCODE=SES_021|observacion", "corr", "cancelarSesionCoordinador")).getCode());
        for (final String codigo : new String[]{"SES_023", "SES_024", "SES_025"}) {
            assertEquals(codigo, assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                    false, "m", "DBCODE=" + codigo + "|solicitud", "corr", "aprobarSolicitudSesion")).getCode());
        }
    }
}
