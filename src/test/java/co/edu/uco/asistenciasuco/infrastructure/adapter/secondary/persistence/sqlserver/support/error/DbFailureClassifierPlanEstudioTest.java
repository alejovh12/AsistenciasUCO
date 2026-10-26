package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HU081-HU083: codigos de DB del estado y los semestres del plan de estudios y su clasificacion HTTP. */
class DbFailureClassifierPlanEstudioTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "quitarSemestrePlanEstudio");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLA_003", "PLA_005", "PLA_006"})
    void reglas_de_semestres_responden_409(final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, clasificar(codigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PLA_002", "PLA_004"})
    void plan_o_semestre_inexistente_responde_404(final String codigo) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar(codigo));
    }
}
