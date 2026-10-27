package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Solicitud de revision propia del estudiante autenticado (HU010/HU011), con los datos minimos del soporte
 * adjunto (si existe). Misma semantica temporal que la bandeja docente: {@code fechaHoraInicioSesionUtc} es nula
 * cuando la hora de la sesion no tiene procedencia UTC verificable.
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
        int totalRegistros
) {
}
