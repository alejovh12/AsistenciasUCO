package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.exception.RevisionCancelacionErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** DEC-065 (1), HU026C: RCN_016 (el decano no resuelve la revision de cancelacion) es 403 con su codigo de catalogo. */
class DbFailureClassifierMrmCancelacionTest {

    private static final String USER = "La resolución de la revisión corresponde al coordinador del programa.";
    private static final String TECNICO = "DBCODE=RCN_016|detalle tecnico mutable";

    @Test
    void rcn_016_es_prohibido_con_su_codigo_de_catalogo() {
        final var rcn016 = DbFailureClassifier.classify(USER, TECNICO, "resolverRevisionCancelacion");

        assertEquals(RevisionCancelacionErrorCode.RCN_016, rcn016);
        assertEquals("RCN_016", rcn016.code());
        assertEquals(ErrorKind.FORBIDDEN, rcn016.kind());
        assertThrows(ForbiddenException.class,
                () -> DbExceptionTranslator.throwIfFailed(false, USER, TECNICO, "c", "resolverRevisionCancelacion"));
    }

    @Test
    void los_codigos_rcn_se_traducen_segun_su_clase_de_error() {
        assertThrows(ResourceNotFoundException.class, () -> lanzar("RCN_001"));
        for (final String codigo : new String[]{"RCN_002", "RCN_003"}) {
            assertThrows(ValidationException.class, () -> lanzar(codigo));
        }
        for (final String codigo : new String[]{"RCN_004", "RCN_005", "RCN_006", "RCN_012", "RCN_013", "RCN_014"}) {
            assertThrows(ConflictException.class, () -> lanzar(codigo));
        }
    }

    @Test
    void rcn_013_conserva_el_texto_de_solicitar_la_asignacion_al_decano() {
        assertEquals("El programa no tiene coordinador activo para resolver la revisión; solicite su asignación al decano.",
                RevisionCancelacionErrorCode.RCN_013.defaultMessage());
    }

    private static void lanzar(final String codigo) {
        DbExceptionTranslator.throwIfFailed(false, USER, "DBCODE=" + codigo + "|detalle", "c", "decidirRevisionCancelacion");
    }
}
