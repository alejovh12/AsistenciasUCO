package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort.FaltaReclamable;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * ATA D10: fila de {@code GET /api/v1/estudiante/faltas-reclamables}. Una fila por toma en falta que el estudiante aun puede reclamar.
 * Las horas son ISO-8601 UTC con {@code Z}; {@code fechaLimiteReclamoUtc} es el instante exclusivo del plazo.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FaltaReclamableResponse(
        UUID idGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idSesion,
        int numeroSesion,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        UUID idToma,
        int numeroToma,
        String fechaPuntoControlUtc,
        String fechaLimiteReclamoUtc
) {

    public static FaltaReclamableResponse from(final FaltaReclamable f) {
        return new FaltaReclamableResponse(f.idGrupo(), f.nombreGrupo(), f.idAsignatura(), f.nombreAsignatura(), f.idSesion(), f.numeroSesion(),
                texto(f.fechaHoraInicioUtc()), texto(f.fechaHoraFinUtc()), f.idToma(), f.numeroToma(), texto(f.fechaPuntoControlUtc()),
                texto(f.fechaLimiteReclamoUtc()));
    }

    private static String texto(final Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
