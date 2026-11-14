package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CULM-001: los codigos propios de la DB (matricula QR, verificacion de correo, admision, tomas y asistencia QR) se publican
 * tal cual; un mensaje sin informacion cae al codigo no clasificado. DEC-074 retiro la carga masiva y sus codigos CMA_*.
 */
class DbFailureClassifierCatalogoCulm001Test {

    @ParameterizedTest
    @ValueSource(strings = {
            "MQR_001", "MQR_004", "MQR_005", "MQR_006", "MQR_007",
            "VCE_001", "VCE_002", "VCE_005", "VCE_007",
            "ADS_002", "ADS_003", "ADS_004", "ADS_005", "ADS_006",
            "TOM_001", "TOM_002", "TOM_003", "TOM_004", "TOM_012",
            "AQR_001", "AQR_002", "AQR_003", "AQR_004", "AQR_005", "AQR_006", "AQR_007", "AQR_010"
    })
    void el_codigo_de_la_db_se_expone_tal_cual(final String codigoDb) {
        final ErrorDefinition clasificado = DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", "operacion");

        assertEquals(codigoDb, clasificado.code());
        assertNotNull(clasificado.kind());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CMA_001", "CMA_009", "CMA_014"})
    void los_codigos_de_la_carga_masiva_retirada_ya_no_se_clasifican(final String codigoDb) {
        // DEC-074: la BD ya no emite CMA_*; si llegara uno es un codigo desconocido (fallo tecnico), no un error de negocio.
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED, DbFailureClassifier.classify("mensaje", "DBCODE=" + codigoDb + "|detalle", "operacion"));
    }

    @Test
    void un_mensaje_vacio_o_nulo_es_no_clasificado() {
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED, DbFailureClassifier.classify(null, null, null));
        assertEquals(DatabaseErrorCode.ERR_DB_UNCLASSIFIED, DbFailureClassifier.classify("  ", "", "operacion"));
    }

    @Test
    void una_violacion_del_catalogo_de_razon_causa_es_estado_de_asistencia_invalido() {
        final ErrorDefinition clasificado = DbFailureClassifier.classify(
                "La restriccion FK_x fallo", "conflicto en la tabla RazonCausa.codigo", "registrarAsistencia");

        assertEquals(co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO, clasificado);
        assertEquals(co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode.ERR_ESTADO_ASISTENCIA_INVALIDO,
                DbFailureClassifier.classify("el estado de asistencia no existe", "razon de causa desconocida", "registrarAsistencia"));
    }
}
