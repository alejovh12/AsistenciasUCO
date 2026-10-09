package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Paridad de conversiones JPA-05 frente a {@link JdbcBaselineValueMapper} (comportamiento BEFORE congelado).
 * Sin base de datos: valida las reglas NULL y de tipo que no pueden cambiar al migrar.
 */
class AcademicViewJpaProjectionMapperTest {

    @Test
    void flag_bit_nulo_es_falso_como_en_jdbc() {
        assertFalse(AcademicViewJpaProjectionMapper.flag((Boolean) null));
        assertEquals(JdbcBaselineValueMapper.toBoolean(null), AcademicViewJpaProjectionMapper.flag((Boolean) null));
        assertTrue(AcademicViewJpaProjectionMapper.flag(Boolean.TRUE));
        assertFalse(AcademicViewJpaProjectionMapper.flag(Boolean.FALSE));
    }

    @Test
    void flag_int_solo_es_verdadero_con_uno_como_en_jdbc() {
        assertFalse(AcademicViewJpaProjectionMapper.flag((Integer) null));
        assertTrue(AcademicViewJpaProjectionMapper.flag(1));
        assertFalse(AcademicViewJpaProjectionMapper.flag(0));
        assertFalse(AcademicViewJpaProjectionMapper.flag(2));
        assertEquals(JdbcBaselineValueMapper.toBoolean(2), AcademicViewJpaProjectionMapper.flag(2));
        assertEquals(JdbcBaselineValueMapper.toBoolean(1), AcademicViewJpaProjectionMapper.flag(1));
    }

    @Test
    void text_conserva_null_y_escribe_numeros_como_en_jdbc() {
        assertNull(AcademicViewJpaProjectionMapper.text(null));
        assertEquals("12345", AcademicViewJpaProjectionMapper.text(12345));
        assertEquals(JdbcBaselineValueMapper.toString(12345), AcademicViewJpaProjectionMapper.text(12345));
    }

    @Test
    void localTime_parsea_varchar_hhmm_y_conserva_null() {
        assertNull(AcademicViewJpaProjectionMapper.localTime(null));
        assertEquals(LocalTime.of(8, 30), AcademicViewJpaProjectionMapper.localTime("08:30"));
        assertEquals(JdbcBaselineValueMapper.toLocalTime("08:30"), AcademicViewJpaProjectionMapper.localTime("08:30"));
    }
}


