package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.features.admin.exception.DecanoErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DEC-065 (5): los codigos de {@code usp_crear_decano_con_facultad} y {@code usp_crear_coordinador_con_programa} llegan al cliente
 * con su HTTP: GEN_001 (institucion o facultad inexistente) 404 solo en estas dos operaciones; el resto ya estaba clasificado.
 */
class DbFailureClassifierAltaCompuestaTest {

    private static final String USER = "Mensaje publico.";

    private static String tecnico(final String codigo) {
        return "DBCODE=" + codigo + "|detalle tecnico mutable";
    }

    @ParameterizedTest
    @CsvSource({"crearDecanoConFacultad", "crearCoordinadorConPrograma"})
    void gen_001_es_recurso_no_encontrado_en_las_altas_compuestas(final String operacion) {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("GEN_001"), operacion));
    }

    @ParameterizedTest
    @CsvSource({"crearDecano", "crearFacultad", "crearProgramaAcademico"})
    void gen_001_sigue_siendo_tecnico_en_otras_operaciones(final String operacion) {
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED, DbFailureClassifier.classify(USER, tecnico("GEN_001"), operacion));
    }

    @ParameterizedTest
    @CsvSource({
            "crearDecanoConFacultad, GEN_003",
            "crearDecanoConFacultad, PROG_003",
            "crearCoordinadorConPrograma, PROG_003",
            "crearCoordinadorConPrograma, PROG_005"
    })
    void duplicados_y_coordinador_ocupado_son_conflicto(final String operacion, final String codigo) {
        assertEquals(CommonErrorCode.CONFLICT, DbFailureClassifier.classify(USER, tecnico(codigo), operacion));
    }

    @ParameterizedTest
    @CsvSource({
            "crearDecanoConFacultad, GEN_002",
            "crearCoordinadorConPrograma, GEN_002",
            "crearCoordinadorConPrograma, PROG_004",
            "crearCoordinadorConPrograma, PROG_006"
    })
    void validaciones_son_400(final String operacion, final String codigo) {
        assertEquals(CommonErrorCode.VALIDATION_ERROR, DbFailureClassifier.classify(USER, tecnico(codigo), operacion));
    }

    @ParameterizedTest
    @CsvSource({
            "crearDecanoConFacultad, SEC_001",
            "crearCoordinadorConPrograma, SEC_001",
            "crearCoordinadorConPrograma, SEC_002"
    })
    void falta_de_permiso_o_de_ambito_es_403(final String operacion, final String codigo) {
        assertEquals(SecurityErrorCode.FORBIDDEN, DbFailureClassifier.classify(USER, tecnico(codigo), operacion));
    }

    @ParameterizedTest
    @CsvSource({"crearDecanoConFacultad", "crearDecano"})
    void decano_ya_registrado_es_dec_020(final String operacion) {
        assertEquals(DecanoErrorCode.DEC_020, DbFailureClassifier.classify(USER, tecnico("DEC_020"), operacion));
    }
}
