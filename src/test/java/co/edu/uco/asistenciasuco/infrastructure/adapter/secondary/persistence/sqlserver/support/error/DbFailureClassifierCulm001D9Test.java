package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.DecanoErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * CULM-001 D.9-a (CONTRACT §11.1/§11.2): DEC_020 (decano activo repetido, 409 con su codigo de catalogo), NTF_020 y NTF_021
 * (notificacion y tipo de plantilla inexistentes, 404) y SEC_002 (grupo inexistente o ajeno, 403).
 */
class DbFailureClassifierCulm001D9Test {

    private static final String USER = "Mensaje publico.";

    private static String tecnico(final String codigo) {
        return "DBCODE=" + codigo + "|detalle tecnico mutable";
    }

    @Test
    void dec_020_es_conflicto_con_su_codigo_de_catalogo() {
        final var dec020 = DbFailureClassifier.classify(USER, tecnico("DEC_020"), "crearDecano");

        assertEquals(DecanoErrorCode.DEC_020, dec020);
        assertEquals("DEC_020", dec020.code());
        assertEquals(ErrorKind.CONFLICT, dec020.kind());
        assertThrows(ConflictException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("DEC_020"), "c", "crearDecano"));
    }

    @Test
    void ntf_020_y_ntf_021_son_recurso_no_encontrado() {
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("NTF_020"), "reintentarNotificacionSaliente"));
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND, DbFailureClassifier.classify(USER, tecnico("NTF_021"), "consultarPlantillaActiva"));
        assertThrows(ResourceNotFoundException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("NTF_020"), "c", "reintentarNotificacionSaliente"));
        assertThrows(ResourceNotFoundException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("NTF_021"), "c", "consultarPlantillaActiva"));
    }

    @Test
    void sec_002_es_prohibido() {
        assertThrows(ForbiddenException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, tecnico("SEC_002"), "c", "validarAccesoReporteGrupo"));
    }
}
