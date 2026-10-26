package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
