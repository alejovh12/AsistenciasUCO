package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** CULM-001 C.1 (contrato 3.2): alerta de inasistencia del estudiante. {@code mensaje} lo resuelve el backend con el catalogo de mensajes. */
public record AlertaInasistenciaResponse(
        UUID id,
        String tipo,
        String nombreTipo,
        String estado,
        String mensaje,
        BigDecimal porcentajeCruce,
        BigDecimal porcentajeActual,
        BigDecimal umbral,
        BigDecimal umbralPerdida,
        BigDecimal horasFalta,
        BigDecimal horasDictadas,
        Instant fechaCruceUtc,
        boolean leida,
        Instant fechaLecturaUtc,
        Referencia grupo,
        Referencia asignatura,
        String estadoMatricula
) {

    /** Grupo (id, codigo, nombre) o asignatura (id, nombre). */
    public record Referencia(UUID id, Integer codigo, String nombre) {
    }
}
