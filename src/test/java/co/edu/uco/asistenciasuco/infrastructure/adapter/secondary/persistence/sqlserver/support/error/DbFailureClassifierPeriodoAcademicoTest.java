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

    /**
     * VAL-pendientes-john PRU-15: una toma, un lote o un reclamo de un periodo ya cerrado (PER_008) es 409 con su codigo y el texto
     * con el nombre del periodo que arma la BD, en cualquiera de las operaciones que la BD rechaza.
     */
    @ParameterizedTest
    @ValueSource(strings = {"abrirTomaAsistencia", "registrarAsistenciasToma", "cerrarTomaAsistencia", "registrarAsistenciasSesion",
        "radicarSolicitudRevisionAsistencia", "resolverSolicitudRevisionAsistencia", "retirarSolicitudRevisionAsistencia"})
    void operacion_sobre_un_periodo_cerrado_responde_409_con_el_mensaje_de_la_base(final String operacion) {
        assertEquals(MensajeUsuarioErrorCode.PER_008,
                DbFailureClassifier.classify("mensaje", "DBCODE=PER_008|detalle", operacion));

        final var mensaje = "El período académico 2026-1 ya fue cerrado; no se pueden registrar ni modificar tomas, asistencias ni reclamos de sus sesiones.";
        final var excepcion = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=PER_008|detalle", "c", operacion));
        assertEquals("PER_008", excepcion.getErrorDefinition().orElseThrow().code());
        assertEquals(mensaje, excepcion.getMessage());
    }

    /** VAL-pendientes-john PRU-15: el segundo cierre masivo de un periodo ya cerrado (PER_009) es 409, no un exito idempotente. */
    @Test
    void segundo_cierre_de_un_periodo_cerrado_responde_409_con_la_fecha_del_primero() {
        assertEquals(MensajeUsuarioErrorCode.PER_009, clasificar("PER_009"));
        assertEquals("PER_009", MensajeUsuarioErrorCode.PER_009.code());

        final var mensaje = "El período académico ya fue cerrado el 18/11/2026; el cierre masivo no se puede repetir.";
        final var excepcion = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=PER_009|detalle", "c", "ejecutarCierreMasivoPeriodo"));
        assertEquals("PER_009", excepcion.getErrorDefinition().orElseThrow().code());
        assertEquals(mensaje, excepcion.getMessage());
    }
}
