package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * NOTOMA-001: aviso a la coordinacion cuando el docente no tomo asistencia en una sesion. Los tres comandos son de sistema (sin usuario):
 * la base de datos decide que sesiones estan pendientes (terminadas, sin ninguna toma, dentro del atraso maximo y sin aviso previo), a
 * que coordinadores se avisa (los de cada programa vigente del grupo, sin repetir personas) y registra la marca de forma idempotente.
 */
public interface AvisoSesionSinTomaRepositoryPort {

    /** Sesiones por avisar en esta pasada (el SP aplica el lote y el atraso maximo). */
    List<SesionSinTomaPendiente> consultarSesionesPendientes();

    /** Una fila por coordinador activo con correo; vacia si la sesion ya no aplica (cancelada o con tomas). */
    List<DestinatarioAvisoSinToma> consultarDestinatarios(UUID sesionId);

    /** Marca la sesion como avisada con el total de coordinadores avisados; idempotente. */
    void registrarAviso(UUID sesionId, int totalCoordinadoresAvisados);

    record SesionSinTomaPendiente(UUID idSesion, UUID idGrupo, Instant fechaHoraFinUtc) {
    }

    record DestinatarioAvisoSinToma(
            UUID idSesion,
            UUID idGrupo,
            UUID idAsignatura,
            String nombreAsignatura,
            String nombreGrupo,
            String nombreDocente,
            Instant fechaHoraInicioUtc,
            Instant fechaHoraFinUtc,
            UUID idUsuarioCoordinador,
            String nombreCoordinador,
            String correoCoordinador
    ) {
    }
}
