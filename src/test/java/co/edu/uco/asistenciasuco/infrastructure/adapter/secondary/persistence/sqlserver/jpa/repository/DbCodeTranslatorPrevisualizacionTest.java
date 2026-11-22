package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Revision final BE-1: cada DBCODE que usp_previsualizar_horario_grupo puede lanzar con THROW 51000 (GEN_002, VAL_001, VAL_002, HOR_004,
 * ASI_010, ASI_014, PER_010, SEC_001, SEC_002) responde un 4xx con su codigo, nunca un 500.
 */
class DbCodeTranslatorPrevisualizacionTest {

    private static RuntimeException traducir(final String codigo) {
        return DbCodeTranslator.traducir(new PersistenceException("wrapper", new RuntimeException("DBCODE=" + codigo + "|detalle tecnico")));
    }

    @Test
    void los_codigos_de_negocio_de_la_previsualizacion_son_4xx_con_su_codigo() {
        assertInstanceOf(ConflictException.class, traducir("ASI_010"));
        assertInstanceOf(ConflictException.class, traducir("ASI_014"));
        assertInstanceOf(ConflictException.class, traducir("PER_010"));
        assertInstanceOf(ValidationException.class, traducir("GEN_002"));
        assertInstanceOf(ValidationException.class, traducir("VAL_001"));
        assertInstanceOf(ValidationException.class, traducir("VAL_002"));
        assertInstanceOf(ValidationException.class, traducir("HOR_004"));
        assertInstanceOf(ForbiddenException.class, traducir("SEC_001"));
        assertInstanceOf(ForbiddenException.class, traducir("SEC_002"));
        assertEquals("ASI_014", ((ApplicationException) traducir("ASI_014")).getCode());
        assertEquals("PER_010", ((ApplicationException) traducir("PER_010")).getCode());
    }
}
