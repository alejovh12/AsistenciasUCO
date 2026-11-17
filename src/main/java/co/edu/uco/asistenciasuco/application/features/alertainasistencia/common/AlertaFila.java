package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertaFila(UUID id, String codigoTipo, String nombreTipo, String codigoEstado, String codigoMensajeUsuario,
                  BigDecimal porcentajeCruce, BigDecimal porcentajeActual, BigDecimal umbral, BigDecimal umbralPerdida,
                  BigDecimal horasFalta, BigDecimal horasDictadas, Instant fechaCruceUtc, boolean leida, Instant fechaLecturaUtc,
                  UUID grupoId, int codigoGrupo, String nombreGrupo, UUID asignaturaId, String nombreAsignatura, String estadoMatricula,
                      String mensaje, String codigoPermanenciaActual) {
    /** Forma previa a CIERRE-W3I (sin el nivel de permanencia). */
    public AlertaFila(final UUID id, final String codigoTipo, final String nombreTipo, final String codigoEstado, final String codigoMensajeUsuario, final BigDecimal porcentajeCruce, final BigDecimal porcentajeActual, final BigDecimal umbral, final BigDecimal umbralPerdida, final BigDecimal horasFalta, final BigDecimal horasDictadas, final Instant fechaCruceUtc, final boolean leida, final Instant fechaLecturaUtc, final UUID grupoId, final int codigoGrupo, final String nombreGrupo, final UUID asignaturaId, final String nombreAsignatura, final String estadoMatricula, final String mensaje) {
        this(id, codigoTipo, nombreTipo, codigoEstado, codigoMensajeUsuario, porcentajeCruce, porcentajeActual, umbral, umbralPerdida, horasFalta, horasDictadas, fechaCruceUtc, leida, fechaLecturaUtc, grupoId, codigoGrupo, nombreGrupo, asignaturaId, nombreAsignatura, estadoMatricula, mensaje, null);
    }


    /** Copia con el mensaje institucional ya resuelto con el catalogo de mensajes. */
    public AlertaFila conMensaje(final String texto) {
        return new AlertaFila(id, codigoTipo, nombreTipo, codigoEstado, codigoMensajeUsuario, porcentajeCruce, porcentajeActual, umbral, umbralPerdida,
                horasFalta, horasDictadas, fechaCruceUtc, leida, fechaLecturaUtc, grupoId, codigoGrupo, nombreGrupo, asignaturaId, nombreAsignatura,
                estadoMatricula, texto, codigoPermanenciaActual);
    }
}
