package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcValueMapperTest {

    @Test
    void toUuid_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toUuid(null));
    }

    @Test
    void toUuid_con_instancia_uuid_la_retorna_directamente() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(uuid, JdbcValueMapper.toUuid(uuid));
    }

    @Test
    void toUuid_con_string_la_convierte() {
        final UUID uuid = UUID.randomUUID();
        assertEquals(uuid, JdbcValueMapper.toUuid(uuid.toString()));
    }

    @Test
    void toInteger_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toInteger(null));
    }

    @Test
    void toInteger_con_number_usa_intValue() {
        assertEquals(42, JdbcValueMapper.toInteger(42L));
        assertEquals(7, JdbcValueMapper.toInteger(7));
    }

    @Test
    void toInteger_con_string_lo_parsea() {
        assertEquals(15, JdbcValueMapper.toInteger("15"));
    }

    @Test
    void toBoolean_con_boolean_lo_retorna() {
        assertTrue(JdbcValueMapper.toBoolean(Boolean.TRUE));
        assertFalse(JdbcValueMapper.toBoolean(Boolean.FALSE));
    }

    @Test
    void toBoolean_con_number_evalua_uno_como_verdadero() {
        assertTrue(JdbcValueMapper.toBoolean(1));
        assertFalse(JdbcValueMapper.toBoolean(0));
    }

    @Test
    void toBoolean_con_string_evalua_true_o_uno() {
        assertTrue(JdbcValueMapper.toBoolean("true"));
        assertTrue(JdbcValueMapper.toBoolean("TRUE"));
        assertTrue(JdbcValueMapper.toBoolean("1"));
        assertFalse(JdbcValueMapper.toBoolean("false"));
    }

    @Test
    void toBoolean_con_otro_tipo_retorna_falso() {
        assertFalse(JdbcValueMapper.toBoolean(3.14));
        assertFalse(JdbcValueMapper.toBoolean(new Object()));
    }

    @Test
    void toLocalDate_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toLocalDate(null));
    }

    @Test
    void toLocalDate_con_localdate_lo_retorna() {
        final LocalDate date = LocalDate.of(2026, 1, 20);
        assertEquals(date, JdbcValueMapper.toLocalDate(date));
    }

    @Test
    void toLocalDate_con_sql_date_lo_convierte() {
        assertEquals(LocalDate.of(2026, 1, 20), JdbcValueMapper.toLocalDate(Date.valueOf("2026-01-20")));
    }

    @Test
    void toLocalDate_con_timestamp_lo_convierte() {
        final Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2026, 1, 20, 8, 0));
        assertEquals(LocalDate.of(2026, 1, 20), JdbcValueMapper.toLocalDate(timestamp));
    }

    @Test
    void toLocalDate_con_localdatetime_lo_convierte() {
        assertEquals(LocalDate.of(2026, 1, 20), JdbcValueMapper.toLocalDate(LocalDateTime.of(2026, 1, 20, 8, 0)));
    }

    @Test
    void toLocalDate_con_string_lo_parsea() {
        assertEquals(LocalDate.of(2026, 1, 20), JdbcValueMapper.toLocalDate("2026-01-20"));
    }

    @Test
    void toLocalDateTime_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toLocalDateTime(null));
    }

    @Test
    void toLocalDateTime_con_localdatetime_lo_retorna() {
        final LocalDateTime ldt = LocalDateTime.of(2026, 1, 20, 8, 0);
        assertEquals(ldt, JdbcValueMapper.toLocalDateTime(ldt));
    }

    @Test
    void toLocalDateTime_con_timestamp_lo_convierte() {
        final LocalDateTime ldt = LocalDateTime.of(2026, 1, 20, 8, 0);
        assertEquals(ldt, JdbcValueMapper.toLocalDateTime(Timestamp.valueOf(ldt)));
    }

    @Test
    void toLocalDateTime_con_string_lo_parsea() {
        assertEquals(LocalDateTime.of(2026, 1, 20, 8, 0), JdbcValueMapper.toLocalDateTime("2026-01-20T08:00:00"));
    }

    private TimeZone originalDefaultTimeZone;

    @AfterEach
    void restoreDefaultTimeZone() {
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
            originalDefaultTimeZone = null;
        }
    }

    /**
     * Contrato TARGET (LB-001B.3, CONTRACT_FREEZE.md secc. 5, punto 2 / punto N de
     * TASK_AUTORIZADA.md &sect;27 y &sect;20-21): {@code Sesion.fechaHoraInicio}/{@code fechaHoraFin}
     * son {@code DATETIME2} con semantica UTC target; ninguna capa debe interpretarlos con
     * {@code ZoneId.systemDefault()} ni asumir {@code America/Bogota}. Prueba de estabilidad: el mismo
     * {@code java.sql.Timestamp} de entrada (construido con {@code TimeZone.getDefault()=UTC}, de
     * forma que sus millis-desde-epoca representan literalmente el wall-clock esperado) debe producir
     * el mismo {@code LocalDateTime} de salida sin importar el {@code user.timezone} de la JVM activo
     * en el momento de la lectura.
     *
     * <p>RED esperado: {@code JdbcValueMapper.toLocalDateTime(Object)} delega en
     * {@code java.sql.Timestamp#toLocalDateTime()}, cuyos campos (year/month/day/hour/minute/second)
     * se derivan internamente vía {@code java.util.Date} usando {@code TimeZone.getDefault()} en el
     * momento de la lectura — no una marca UTC explícita. Al construir el {@code Timestamp} bajo
     * {@code TimeZone=UTC} y leerlo bajo {@code TimeZone=America/Bogota} (UTC-05:00), el
     * {@code LocalDateTime} resultante se desplaza ~5 horas respecto del valor original, revelando la
     * dependencia oculta de {@code systemDefault()} que CONTRACT_FREEZE.md secc. 5 exige eliminar.</p>
     */
    @Test
    void toLocalDateTime_no_depende_del_systemDefault_de_la_jvm_para_datetime2_utc_de_sesion() {
        originalDefaultTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            final LocalDateTime valorEscritoComoUtc = LocalDateTime.of(2026, 6, 15, 8, 30, 0);
            final Timestamp timestampDesdeJdbc = Timestamp.valueOf(valorEscritoComoUtc);

            TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
            final LocalDateTime resultadoBajoOtroTimezone = JdbcValueMapper.toLocalDateTimeUtc(timestampDesdeJdbc);

            assertEquals(valorEscritoComoUtc, resultadoBajoOtroTimezone,
                    "Sesion.fechaHoraInicio/fechaHoraFin debe leerse igual sin importar user.timezone de la "
                            + "JVM (DB_BASELINE_CONTRACT.md declara DATETIME2 con semantica UTC; prohibido "
                            + "ZoneId.systemDefault()/TimeZone.getDefault() implicito, CONTRACT_FREEZE.md secc. 5).");
        } finally {
            TimeZone.setDefault(originalDefaultTimeZone);
            originalDefaultTimeZone = null;
        }
    }

    @Test
    void toLocalTime_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toLocalTime(null));
    }

    @Test
    void toLocalTime_con_localtime_lo_retorna() {
        final LocalTime time = LocalTime.of(8, 0);
        assertEquals(time, JdbcValueMapper.toLocalTime(time));
    }

    @Test
    void toLocalTime_con_sql_time_lo_convierte() {
        assertEquals(LocalTime.of(8, 0), JdbcValueMapper.toLocalTime(Time.valueOf("08:00:00")));
    }

    @Test
    void toLocalTime_con_string_lo_parsea() {
        assertEquals(LocalTime.of(8, 0), JdbcValueMapper.toLocalTime("08:00:00"));
    }

    @Test
    void toString_con_valor_nulo_retorna_nulo() {
        assertNull(JdbcValueMapper.toString(null));
    }

    @Test
    void toString_con_valor_no_nulo_lo_convierte() {
        assertEquals("42", JdbcValueMapper.toString(42));
    }
}
