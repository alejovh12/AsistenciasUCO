package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Conversiones de valores provenientes de JDBC.
 */
public final class JdbcValueMapper {

    private JdbcValueMapper() {
    }

    public static UUID toUuid(final Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    public static Integer toInteger(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    public static boolean toBoolean(final Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 1;
        }
        if (value instanceof String stringValue) {
            return "true".equalsIgnoreCase(stringValue) || "1".equals(stringValue);
        }
        return false;
    }

    public static LocalDate toLocalDate(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    /**
     * Convierte un valor JDBC a {@link LocalDateTime}.
     *
     * <p><b>Semantica UTC para {@code Sesion.fechaHoraInicio}/{@code fechaHoraFin} (LB-001B.3,
     * CONTRACT_FREEZE.md secc. 5):</b> el contrato DB congelado (`DB_BASELINE_CONTRACT.md`,
     * seccion Temporal) declara esas dos columnas como {@code DATETIME2} con semantica UTC target.
     * El {@link LocalDateTime} que este metodo retorna para esas columnas debe interpretarse como
     * un instante UTC por convencion del contrato; ninguna capa debe aplicarle
     * {@code ZoneId.systemDefault()} ni asumir zona horaria local (p. ej. America/Bogota) al
     * consumirlo. Los 3 unicos llamadores actuales de este metodo en produccion
     * ({@code SesionRepositorySqlServerAdapter}, {@code ReporteAsistenciaSqlServerAdapter},
     * {@code SesionMateriaEstudianteSqlServerAdapter}) lo usan exclusivamente para
     * {@code fechaHoraInicio}/{@code fechaHoraFin} provenientes de {@code uv_sesion}; no hay hoy
     * ningun campo no-Sesion compartiendo este metodo.</p>
     *
     * <p><b>TEST_CONTRACT_CONFLICT documentado (no resuelto en esta fase, ver VALIDATION.md):</b>
     * {@code java.sql.Timestamp#toLocalDateTime()} deriva sus campos via los accesores heredados de
     * {@code java.util.Date}, que recalculan year/month/day/hour/minute/second usando
     * {@code TimeZone.getDefault()} <i>en el momento de la lectura</i>, no en el de la escritura —
     * una dependencia oculta real de {@code systemDefault()} (demostrada por
     * {@code JdbcValueMapperTest.toLocalDateTime_no_depende_del_systemDefault_de_la_jvm_para_datetime2_utc_de_sesion}).
     * Reemplazar la conversion por {@code timestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime()}
     * elimina esa dependencia para ese test aislado, pero se verifico empiricamente que rompe 2
     * pruebas congeladas adicionales que ya son GREEN/MATCH (round-trip generico
     * {@code JdbcValueMapperTest.toLocalDateTime_con_timestamp_lo_convierte} y
     * {@code SesionRepositorySqlServerAdapterTest.queryMapsConfirmedViewColumnsAndReturnsNullWhenAbsent},
     * cobertura confirmada del punto C de TASK_AUTORIZADA.md &sect;27, "ya MATCH") en cualquier JVM
     * cuyo {@code TimeZone.getDefault()} de proceso no sea UTC — el caso real de esta maquina de
     * build ({@code America/Bogota}). No existe una funcion pura de {@code Timestamp} que satisfaga
     * ambos contratos de prueba simultaneamente bajo esa condicion; por tanto el comportamiento de
     * este metodo permanece AS-IS (sin cambio de codigo) hasta que 02-contratos/03-tester-red
     * resuelvan el conflicto (p. ej. fijando {@code user.timezone=UTC} en la configuracion de build,
     * o revisando la prueba round-trip para forzar UTC explicitamente).</p>
     */
    public static LocalDateTime toLocalDateTime(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    /**
     * Convierte un valor JDBC a {@link LocalDateTime} interpretando siempre el instante como UTC,
     * ignorando el {@code TimeZone.getDefault()}/{@code user.timezone} vigente en el momento de la
     * lectura. Uso exclusivo de {@code Sesion.fechaHoraInicio}/{@code fechaHoraFin}
     * ({@code DATETIME2} UTC target, DB_BASELINE_CONTRACT.md secc. Temporal) desde sus 3 llamadores
     * confirmados: {@code SesionRepositorySqlServerAdapter}, {@code ReporteAsistenciaSqlServerAdapter},
     * {@code SesionMateriaEstudianteSqlServerAdapter}. No usar para ningun otro campo temporal
     * (p. ej. {@code Horario.horaInicio/horaFin}, que es hora academica LOCAL, no UTC).
     */
    public static LocalDateTime toLocalDateTimeUtc(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    public static LocalTime toLocalTime(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalTime localTime) {
            return localTime;
        }
        if (value instanceof java.sql.Time time) {
            return time.toLocalTime();
        }
        return LocalTime.parse(String.valueOf(value));
    }

    public static String toString(final Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
