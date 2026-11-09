package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Solicitud de revision propia del estudiante autenticado (HU010/HU011), con los datos minimos del soporte
 * adjunto (si existe). Misma semantica temporal que la bandeja docente: {@code fechaHoraInicioSesionUtc} es nula
 * cuando la hora de la sesion no tiene procedencia UTC verificable. CULM-001 D.2: {@code codigoCategoria}/{@code nombreCategoria}
 * (nulos en reclamos anteriores a la categoria persistida) y {@code soportes} (todos los soportes, en orden de adjuncion; las
 * columnas de soporte unico siguen siendo las del primero).
 */
public record SolicitudRevisionEstudianteProjection(
        UUID idSolicitud,
        String nombreSolicitud,
        LocalDate fechaSolicitud,
        String codigoEstado,
        String nombreEstado,
        String justificacionSolicitud,
        String justificacionRespuesta,
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
        UUID idArchivoSoporte,
        String nombreSoporte,
        String tipoContenidoSoporte,
        Long tamanioSoporteBytes,
        Instant fechaRegistroSoporteUtc,
        int totalRegistros,
        String codigoCategoria,
        String nombreCategoria,
        List<SoporteReclamoProjection> soportes
) {
}
