package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
