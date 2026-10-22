package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.application.exception.common.CommonErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** T-ERR-01: codigos de DB del programa academico y su clasificacion HTTP (CONTRACT §2.8). */
class DbFailureClassifierProgramaTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "crearProgramaAcademico");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROG_002", "PROG_003", "PROG_005", "PROG_007", "PROG_008"})
    void conflictos_de_negocio_responden_409(final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROG_004", "PROG_006", "PAG_001"})
    void entradas_invalidas_responden_400(final String codigo) {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROG_001"})
    void programa_inexistente_responde_404(final String codigo) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar(codigo));
    }
}
