package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ATA D1 (HU175, ADDENDUM_ATA_ASISTENCIA_TOMAS_AVISOS C): aviso de falta por sesion. Los tres comandos son de sistema (sin usuario):
 * la base de datos decide que sesiones estan pendientes (terminadas, con todas sus tomas cerradas, dentro del atraso maximo y sin
 * aviso previo), a quien se avisa y registra la marca de sesion avisada de forma idempotente.
 */
public interface AvisoFaltaSesionRepositoryPort {

    /** Sesiones por avisar en esta pasada (el SP aplica el lote y el atraso maximo). */
    List<SesionPendienteAviso> consultarSesionesPendientes();

    /** Una fila por (estudiante con SJC, toma en SJC) de la sesion; solo matriculas activas con correo. */
    List<FaltaSesionAviso> consultarFaltas(UUID sesionId);

    /** Marca la sesion como avisada con el total de estudiantes avisados; idempotente. */
    void registrarAviso(UUID sesionId, int totalEstudiantesAvisados);

    record SesionPendienteAviso(UUID idSesion, UUID idGrupo, Instant fechaHoraFinUtc) {
    }

    record FaltaSesionAviso(
            UUID idSesion,
            UUID idGrupo,
            UUID idAsignatura,
            String nombreAsignatura,
            String nombreGrupo,
            Instant fechaHoraInicioUtc,
            Instant fechaHoraFinUtc,
            UUID idUsuarioEstudiante,
            String nombreEstudiante,
            String correoEstudiante,
            int numeroToma,
            Instant fechaPuntoControlUtc,
            int totalTomasSesion,
            int totalTomasEnFalta,
            int minutosFalta,
            Instant fechaLimiteReclamoUtc
    ) {
    }
}
