package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.crosscutting.exception.TechnicalException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * LB-008 JPA-01 foundation: el validator recibe el contrato {@link ProcedureResult}, no conoce la
 * implementación concreta ni cardinalidad de filas, y delega la traducción funcional a
 * {@link DbExceptionTranslator}.
 */
class ProcedureResultValidatorTest {

    private static final String OPERATION = "registrarAsistenciasSesion";
    private static final UUID CORRELATION = UUID.randomUUID();
    private static final String NULL_MESSAGE = "El procedimiento no retorno resultado.";
    private static final String CORRELATION_MESSAGE = "La correlacion retornada por la DB no coincide con la peticion.";

    @Test
    void resultado_exitoso_con_correlacion_coincidente_se_retorna_por_el_contrato_comun() {
        final ProcedureResult result = result(CORRELATION, true, "detalle");

        assertSame(result, ProcedureResultValidator.validate(result, CORRELATION, OPERATION));
    }

    @Test
    void resultado_nulo_es_violacion_del_contrato_canonico() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> ProcedureResultValidator.validate(null, CORRELATION, OPERATION));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(NULL_MESSAGE, exception.getMessage());
    }

    @Test
    void correlacion_devuelta_distinta_es_violacion_del_contrato_canonico() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> ProcedureResultValidator.validate(
                        result(UUID.randomUUID(), true, "detalle"), CORRELATION, OPERATION));

        assertEquals(DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT.code(), exception.getCode());
        assertEquals(CORRELATION_MESSAGE, exception.getMessage());
    }

    @Test
    void correlacion_nula_devuelta_no_coincide_con_la_esperada() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> ProcedureResultValidator.validate(result(null, true, "detalle"), CORRELATION, OPERATION));

        assertEquals(CORRELATION_MESSAGE, exception.getMessage());
    }

    @Test
    void correlacion_se_valida_antes_que_el_estado_funcional() {
        final DatabaseOperationException exception = assertThrows(DatabaseOperationException.class,
                () -> ProcedureResultValidator.validate(
                        result(UUID.randomUUID(), false, "DBCODE=SEC_001|x"), CORRELATION, OPERATION));

        assertEquals(CORRELATION_MESSAGE, exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "DBCODE=SEC_001|d", "DBCODE=SEC_002|d", "DBCODE=EST_004|d", "DBCODE=ATT_001|d",
            "DBCODE=ATT_002|d", "DBCODE=ATT_003|d", "DBCODE=GEN_002|d", "DBCODE=RC_001|d",
            "DBCODE=SES_004|d", "DBCODE=SES_001|d", "DBCODE=SES_003|d", "DBCODE=XYZ_999|d",
            "DBCODE=sec-001|malformado", "texto legacy sin marcador"
    })
    void estado_false_delega_la_traduccion_a_db_exception_translator(final String technicalMessage) {
        final ProcedureResult failure = result(CORRELATION, false, technicalMessage);

        final RuntimeException viaValidator = assertThrows(RuntimeException.class,
                () -> ProcedureResultValidator.validate(failure, CORRELATION, OPERATION));
        final RuntimeException viaTranslator = assertThrows(RuntimeException.class,
                () -> DbExceptionTranslator.throwIfFailed(
                        false, failure.getMensajeUsuarioResultado(), technicalMessage,
                        CORRELATION.toString(), OPERATION));

        assertEquals(viaTranslator.getClass(), viaValidator.getClass());
        assertEquals(codeOf(viaTranslator), codeOf(viaValidator));
        assertEquals(viaTranslator.getMessage(), viaValidator.getMessage());
    }

    private static ProcedureResult result(
            final UUID correlation,
            final boolean successful,
            final String technicalMessage
    ) {
        return new ProcedureResult() {
            @Override
            public UUID getIdCorrelacion() {
                return correlation;
            }

            @Override
            public String getMensajeUsuarioResultado() {
                return "mensaje usuario";
            }

            @Override
            public String getMensajeTecnicoResultado() {
                return technicalMessage;
            }

            @Override
            public boolean getEstadoResultado() {
                return successful;
            }
        };
    }

    private static String codeOf(final RuntimeException exception) {
        if (exception instanceof TechnicalException technical) {
            return technical.getCode();
        }
        if (exception instanceof ApplicationException application) {
            return application.getCode();
        }
        return exception.getClass().getName();
    }
}


