package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain;

import java.time.Instant;
import java.util.UUID;

public record BitacoraSesionDomain(
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
