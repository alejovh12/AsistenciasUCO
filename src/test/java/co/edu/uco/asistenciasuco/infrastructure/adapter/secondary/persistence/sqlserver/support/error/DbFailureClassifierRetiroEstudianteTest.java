package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HU051: codigos de DB del retiro de un estudiante del grupo y su clasificacion HTTP (CL-01..CL-03). */
class DbFailureClassifierRetiroEstudianteTest {

    private static ErrorDefinition clasificar(final String codigo) {
        return DbFailureClassifier.classify("mensaje", "DBCODE=" + codigo + "|detalle", "retirarEstudianteGrupo");
    }

    /** RET_001 (grupo inexistente o ejecutor no titular, indistinguibles) y RET_002 (sin matricula activa). */
    @ParameterizedTest
    @ValueSource(strings = {"RET_001", "RET_002"})
    void CL01_grupo_ajeno_o_sin_matricula_activa_responden_404(final String codigo) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, clasificar(codigo));
    }

    @Test
    void CL02_periodo_terminado_responde_409() {
        assertEquals(CommonErrorCode.CONFLICT, clasificar("RET_003"));
    }

    @Test
    void CL03_motivo_invalido_responde_400() {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, clasificar("RET_004"));
    }
}
