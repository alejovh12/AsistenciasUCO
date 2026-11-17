package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HU096-HU098: codigos de DB de periodos academicos y su clasificacion HTTP. */
class DbFailureClassifierPeriodoAcademicoTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "eliminarPeriodoAcademico");
    }

    @Test
    void periodo_inexistente_responde_404() {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar("PER_001"));
    }

    @Test
    void fechas_invalidas_responden_400() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, clasificar("PER_002"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PER_003", "PER_004"})
    void nombre_duplicado_o_periodo_con_grupos_responden_409(final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, clasificar(codigo));
    }

    @Test
    void ejecutor_sin_ambito_responde_403() {
        assertEquals(SecurityErrorCode.FORBIDDEN, clasificar("PER_005"));
    }

    /** FIX-CIERRE-002: PER_007 (periodo que aun no termino) es 409 con su codigo y el texto con la fecha de fin que arma la BD. */
    @Test
    void periodo_que_aun_no_termino_responde_409_con_el_mensaje_de_la_base() {
        assertEquals(MensajeUsuarioErrorCode.PER_007, clasificar("PER_007"));
        assertEquals("PER_007", MensajeUsuarioErrorCode.PER_007.code());

        final var mensaje = "El período académico aún no ha finalizado; el cierre masivo solo puede ejecutarse después de su fecha de fin (07/12/2026).";
        final var excepcion = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=PER_007|detalle", "c", "ejecutarCierreMasivoPeriodo"));
        assertEquals("PER_007", excepcion.getErrorDefinition().orElseThrow().code());
        assertEquals(mensaje, excepcion.getMessage());
    }
}
