package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.business.BusinessException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/** T-ERR-01: codigos DOC_* de la vertical de docentes del programa. */
class DbExceptionTranslatorDocenteProgramaTest {

    private static void fallo(final String dbCode) {
        DbExceptionTranslator.throwIfFailed(false, "mensaje", "DBCODE=" + dbCode + "|detalle", "corr", "vincularDocentePrograma");
    }

    @Test
    void doc_001_y_doc_008_son_recurso_no_encontrado() {
        assertThrows(ResourceNotFoundException.class, () -> fallo("DOC_001"));
        assertThrows(ResourceNotFoundException.class, () -> fallo("DOC_008"));
    }

    @Test
    void doc_002_doc_003_doc_006_y_doc_009_son_conflicto() {
        assertThrows(ConflictException.class, () -> fallo("DOC_002"));
        assertThrows(ConflictException.class, () -> fallo("DOC_003"));
        assertThrows(ConflictException.class, () -> fallo("DOC_006"));
        assertThrows(ConflictException.class, () -> fallo("DOC_009"));
    }

    @Test
    void doc_004_doc_005_y_doc_007_son_validacion() {
        assertThrows(ValidationException.class, () -> fallo("DOC_004"));
        assertThrows(ValidationException.class, () -> fallo("DOC_005"));
        assertThrows(ValidationException.class, () -> fallo("DOC_007"));
    }

    @Test
    void cada_codigo_DOC_se_publica_con_su_codigo_de_feature_para_que_el_cliente_muestre_el_mensaje_del_backend() {
        assertCodigo("DOC_001", "ERR_DOCENTE_NO_EXISTE");
        assertCodigo("DOC_002", "ERR_DOCENTE_YA_REGISTRADO");
        assertCodigo("DOC_003", "ERR_PERSONA_YA_REGISTRADA");
        assertCodigo("DOC_004", "ERR_TIPO_VINCULACION_INVALIDO");
        assertCodigo("DOC_005", "ERR_MODO_VINCULACION_INVALIDO");
        assertCodigo("DOC_006", "ERR_DOCENTE_YA_VINCULADO_PROGRAMA");
        assertCodigo("DOC_007", "ERR_BUSQUEDA_CANDIDATOS_CORTA");
        assertCodigo("DOC_008", "ERR_DOCENTE_NO_VINCULADO_PROGRAMA");
        assertCodigo("DOC_009", "ERR_DOCENTE_SIN_VINCULO_ACTIVO");
    }

    private static void assertCodigo(final String dbCode, final String esperado) {
        final BusinessException exception = assertThrows(BusinessException.class, () -> fallo(dbCode));
        org.junit.jupiter.api.Assertions.assertEquals(esperado, exception.getCode());
    }
}
