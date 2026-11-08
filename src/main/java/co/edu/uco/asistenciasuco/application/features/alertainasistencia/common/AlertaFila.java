package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertaFila(UUID id, String codigoTipo, String nombreTipo, String codigoEstado, String codigoMensajeUsuario,
                  BigDecimal porcentajeCruce, BigDecimal porcentajeActual, BigDecimal umbral, BigDecimal umbralPerdida,
                  BigDecimal horasFalta, BigDecimal horasDictadas, Instant fechaCruceUtc, boolean leida, Instant fechaLecturaUtc,
                  UUID grupoId, int codigoGrupo, String nombreGrupo, UUID asignaturaId, String nombreAsignatura, String estadoMatricula,
                      String mensaje) {

    /** Copia con el mensaje institucional ya resuelto con el catalogo de mensajes. */
    public AlertaFila conMensaje(final String texto) {
        return new AlertaFila(id, codigoTipo, nombreTipo, codigoEstado, codigoMensajeUsuario, porcentajeCruce, porcentajeActual, umbral, umbralPerdida,
                horasFalta, horasDictadas, fechaCruceUtc, leida, fechaLecturaUtc, grupoId, codigoGrupo, nombreGrupo, asignaturaId, nombreAsignatura,
                estadoMatricula, texto);
    }
}
