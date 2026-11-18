package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * ATA D7: fila de {@code GET /api/v1/estudiante/sesiones/calendario} y {@code GET /api/v1/docente/sesiones/calendario}. Las horas son
 * ISO-8601 UTC con {@code Z}; {@code motivoCancelacion} y {@code nombreDiaFestivo} los entrega la DB (nulos si no aplican).
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record SesionCalendarioResponse(
        UUID idSesion,
        UUID idGrupo,
        Integer codigoGrupo,
        String nombreGrupo,
        UUID idAsignatura,
        String codigoAsignatura,
        String nombreAsignatura,
        String nombreDocente,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        boolean esExtraordinaria,
        boolean estaCancelada,
        String motivoCancelacion,
        boolean esCancelacionAutomatica,
        String nombreDiaFestivo
) {

    public static SesionCalendarioResponse from(final SesionCalendario s) {
        return new SesionCalendarioResponse(s.idSesion(), s.idGrupo(), s.codigoGrupo(), s.nombreGrupo(), s.idAsignatura(),
                s.codigoAsignatura(), s.nombreAsignatura(), s.nombreDocente(), texto(s.fechaHoraInicioUtc()), texto(s.fechaHoraFinUtc()),
                s.esExtraordinaria(), s.estaCancelada(), s.motivoCancelacion(), s.esCancelacionAutomatica(), s.nombreDiaFestivo());
    }

    private static String texto(final Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
