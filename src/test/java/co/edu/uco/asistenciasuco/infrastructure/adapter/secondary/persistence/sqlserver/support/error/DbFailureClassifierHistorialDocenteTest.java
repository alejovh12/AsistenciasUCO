package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** HU094: GEN_001 del historial (docente sin vinculo con el programa o inexistente) es 404; en otra operacion sigue tecnico. */
class DbFailureClassifierHistorialDocenteTest {

    @Test
    void CLH01_gen001_del_historial_del_docente_es_404() {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND,
                DbFailureClassifier.classify("mensaje", "DBCODE=GEN_001|detalle", "consultarHistorialGruposDocente"));
    }

    @Test
    void CLH02_gen001_de_otra_operacion_sigue_sin_clasificar() {
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED,
                DbFailureClassifier.classify("mensaje", "DBCODE=GEN_001|detalle", "operacionDesconocida"));
    }
}
