package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LB-008 JPA-01 (RED): el mapper unico convierte la fila canonica nativa ({@code row[0..3]}) en
 * {@link ProcedureResult} validando forma (cardinalidad, columnas, UUID y BIT). No evalua
 * {@code estadoResultado}: esa evaluacion es responsabilidad de {@link ProcedureResultValidator}.
 */
class ProcedureResultMapperTest {

    private static final UUID CORRELATION = UUID.randomUUID();
    private static final String CONTRACT_MESSAGE = "El procedimiento no retorno el contrato canonico esperado.";

    private static Object[] fila(final Object id, final Object mensajeUsuario, final Object mensajeTecnico, final Object estado) {
        return new Object[]{id, mensajeUsuario, mensajeTecnico, estado};
    }

    @Test
    void mapea_una_unica_fila_de_cuatro_columnas_a_procedure_result() {
        final ProcedureResult result = ProcedureResultMapper.mapSingle(
                Collections.singletonList(fila(CORRELATION, "Registro exitoso", "", true)));

        assertEquals(CORRELATION, result.getIdCorrelacion());
        assertEquals("Registro exitoso", result.getMensajeUsuarioResultado());
        assertEquals("", result.getMensajeTecnicoResultado());
        assertTrue(result.getEstadoResultado());
    }

    @Test
    void acepta_uuid_en_texto_y_bit_sql_como_numero() {
        final ProcedureResult result = ProcedureResultMapper.mapSingle(
                Collections.singletonList(fila(CORRELATION.toString(), "u", "t", 0)));

        assertEquals(CORRELATION, result.getIdCorrelacion());
        assertFalse(result.getEstadoResultado());
    }

    @Test
    void conserva_mensajes_nulos_sin_sustituirlos() {
        final ProcedureResult result = ProcedureResultMapper.mapSingle(
                Collections.singletonList(fila(CORRELATION, null, null, true)));

        assertNull(result.getMensajeUsuarioResultado());
        assertNull(result.getMensajeTecnicoResultado());
    }

    @Test
    void estado_false_no_se_traduce_aqui_y_lo_evalua_el_validador_comun() {
        final ProcedureResult result = ProcedureResultMapper.mapSingle(
                Collections.singletonList(fila(CORRELATION, "usuario", "tecnico", false)));

        assertFalse(result.getEstadoResultado());
        assertEquals("usuario", result.getMensajeUsuarioResultado());
    }

    @Test
    void lista_nula_o_vacia_es_violacion_del_contrato_canonico() {
        assertContratoInvalido(null);
        assertContratoInvalido(List.of());
    }

    @Test
    void dos_filas_es_violacion_del_contrato_canonico_y_no_toma_la_primera() {
        assertContratoInvalido(List.of(
                fila(CORRELATION, "a", "a", true),
                fila(CORRELATION, "b", "b", true)));
    }

    @Test
    void fila_con_columnas_distintas_de_cuatro_es_violacion_del_contrato_canonico() {
        assertContratoInvalido(Collections.singletonList(new Object[]{CORRELATION, "u", "t"}));
        assertContratoInvalido(Collections.singletonList(new Object[]{CORRELATION, "u", "t", true, "extra"}));
    }

    @Test
    void elemento_que_no_es_fila_de_columnas_es_violacion_del_contrato_canonico() {
        assertContratoInvalido(List.of("no es una fila"));
    }

    @Test
    void idCorrelacion_nulo_o_no_uuid_es_violacion_del_contrato_canonico() {
        assertContratoInvalido(Collections.singletonList(fila(null, "u", "t", true)));
        assertContratoInvalido(Collections.singletonList(fila("no-es-uuid", "u", "t", true)));
    }

    @Test
    void estadoResultado_nulo_o_fuera_de_bit_es_violacion_del_contrato_canonico() {
        assertContratoInvalido(Collections.singletonList(fila(CORRELATION, "u", "t", null)));
        assertContratoInvalido(Collections.singletonList(fila(CORRELATION, "u", "t", "maybe")));
        assertContratoInvalido(Collections.singletonList(fila(CORRELATION, "u", "t", 2)));
    }

    @Test
    void el_resultado_mapeado_es_compatible_con_la_interfaz_comun() {
        final ProcedureResult result = ProcedureResultMapper.mapSingle(
                Collections.singletonList(fila(CORRELATION, "u", "t", true)));

        assertTrue(result instanceof CanonicalProcedureResult);
    }

    private static void assertContratoInvalido(final List<?> rows) {
        final DatabaseOperationException exception = assertThrows(
                DatabaseOperationException.class,
                () -> ProcedureResultMapper.mapSingle(rows));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(CONTRACT_MESSAGE, exception.getMessage());
    }
}


