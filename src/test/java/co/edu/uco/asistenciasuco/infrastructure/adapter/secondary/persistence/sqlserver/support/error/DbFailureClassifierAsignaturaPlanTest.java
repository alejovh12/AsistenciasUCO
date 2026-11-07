package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** CULM-001 B2NM (CONTRACT 2.8.1): los 11 codigos nuevos y su clasificacion HTTP. */
class DbFailureClassifierAsignaturaPlanTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "asociarAsignaturaPlan");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASI_002", "ASI_003", "ASI_005", "ASI_006", "MQR_008"})
    void conflictos_responden_409(final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASI_007", "EST_005", "EST_006", "COO_002"})
    void validaciones_responden_400(final String codigo) {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ASI_004"})
    void vinculo_inexistente_responde_404(final String codigo) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"COO_003"})
    void programa_ajeno_responde_403(final String codigo) {
        assertEquals(SecurityErrorCode.FORBIDDEN, clasificar(codigo));
    }
}
