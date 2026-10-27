package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.time.Instant;
import java.util.UUID;

/** Sesion vencida de la bitacora de un estudiante (HU067); asistio es nulo si no hay registro. */
public record BitacoraSesionProjection(
        UUID idSesion,
        int numeroSesion,
        String nombreSesion,
        Instant fechaHoraInicio,
        Instant fechaHoraFin,
        UUID idGrupo,
        int codigoGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        Boolean asistio,
        String codigoRazonCausa,
        String nombreRazonCausa
) {
}
