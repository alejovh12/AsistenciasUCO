package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Horario de las sesiones "de reserva" que crean las fixtures de integracion antes de llevarlas a la ventana viva.
 * <p>
 * DEC-072 / DEC-073 (SES_028, 2026-11-13): {@code usp_validar_sesion_mismo_dia_interno} exige que una sesion empiece y termine
 * el mismo dia en la zona institucional ({@code TIEMPO/ZONA_HORARIA_SQLSERVER}, Bogota). Las fixtures creaban la reserva en
 * "ahora + 1 h" durante 2 h, asi que entre las 21:00 y la medianoche de Bogota la sesion cruzaba de dia y todas las IT que las
 * usan fallaban segun la hora del CI. Este ayudante conserva el primer hueco libre y, si la sesion cruzaria la medianoche
 * local, la corre al primer minuto del dia siguiente (siempre hacia adelante, asi que sigue libre de solapes).
 */
public final class SesionDePruebaMismoDia {

    /** Zona institucional (equivale a {@code SA Pacific Standard Time} del parametro de la BD). */
    public static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");

    private SesionDePruebaMismoDia() {
    }

    /**
     * @param inicioUtc inicio propuesto en UTC (como lo recibe el puerto con el contrato v2)
     * @param duracion  duracion de la sesion, menor a un dia
     * @return el mismo inicio si la sesion termina el mismo dia local; si no, el primer minuto del dia local siguiente, en UTC
     */
    public static LocalDateTime inicioSinCruzarMedianoche(final LocalDateTime inicioUtc, final Duration duracion) {
        Objects.requireNonNull(inicioUtc, "El inicio es obligatorio.");
        Objects.requireNonNull(duracion, "La duracion es obligatoria.");
        if (duracion.isNegative() || duracion.compareTo(Duration.ofHours(23)) > 0) {
            throw new IllegalArgumentException("La duracion de una sesion de prueba debe estar entre 0 y 23 horas.");
        }
        final ZonedDateTime inicioLocal = inicioUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONA_INSTITUCIONAL);
        final ZonedDateTime finLocal = inicioLocal.plus(duracion);
        if (finLocal.toLocalDate().equals(inicioLocal.toLocalDate())) {
            return inicioUtc;
        }
        return inicioLocal.toLocalDate().plusDays(1).atStartOfDay(ZONA_INSTITUCIONAL).plusMinutes(1)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
