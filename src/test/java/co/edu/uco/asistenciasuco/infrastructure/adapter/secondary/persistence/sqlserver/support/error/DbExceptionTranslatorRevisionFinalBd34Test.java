package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BE-4 (revision final P3): codigos nuevos de la BD (BD-3 periodos solapados, BD-4 matricula manual con programa comun). */
class DbExceptionTranslatorRevisionFinalBd34Test {

    @Test
    void per_012_es_409_y_conserva_el_mensaje_de_la_db_con_el_nombre_del_periodo() {
        final String mensaje = "Las fechas del período se cruzan con el período 2026-2; los períodos de la institución no pueden solaparse.";
        final ConflictException exception = assertThrows(ConflictException.class, () -> DbExceptionTranslator.throwIfFailed(
                false, mensaje, "DBCODE=PER_012|solape", "corr", "crearPeriodoAcademico"));

        assertEquals("PER_012", exception.getCode());
        assertEquals(mensaje, exception.getMessage());
    }

    @Test
    void est_010_y_est_011_son_400_con_su_codigo_y_mensaje() {
        for (final String codigo : new String[]{"EST_010", "EST_011"}) {
            final String mensaje = "Mensaje de la DB para " + codigo;
            final ValidationException exception = assertThrows(ValidationException.class, () -> DbExceptionTranslator.throwIfFailed(
                    false, mensaje, "DBCODE=" + codigo + "|detalle", "corr", "registrarEstudianteEnGrupo"));

            assertEquals(codigo, exception.getCode());
            assertEquals(mensaje, exception.getMessage());
        }
    }
}
