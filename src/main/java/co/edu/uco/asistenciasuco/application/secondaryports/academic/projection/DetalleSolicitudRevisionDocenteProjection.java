package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detalle de una solicitud de revision de asistencia visible para el docente titular (HU036): datos de la
 * solicitud, estudiante, sesion, grupo y asignatura, causal de la asistencia ({@code asistio} y {@code RazonCausa}) y
 * metadatos del soporte adjunto. Causal y soporte son nulos si no existe detalle de asistencia o archivo adjunto.
 * Los instantes son UTC; {@code fechaHoraInicioSesionUtc} es nula si {@code procedenciaTemporalSesion} es nula.
 * CULM-001 D.2: {@code codigoCategoria}/{@code nombreCategoria} (nulos en reclamos anteriores a la categoria persistida) y
 * {@code soportes} (todos, en orden de adjuncion; las columnas de soporte unico siguen siendo las del primero).
 */
public record DetalleSolicitudRevisionDocenteProjection(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
        UUID idEstudiante,
        String nombreEstudiante,
        UUID idSesion,
        Integer numeroSesion,
        Instant fechaHoraInicioSesionUtc,
        String procedenciaTemporalSesion,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        Boolean asistio,
        String codigoRazonCausa,
        String nombreRazonCausa,
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc,
        String codigoCategoria,
        String nombreCategoria,
        List<SoporteReclamoProjection> soportes
) {
}
