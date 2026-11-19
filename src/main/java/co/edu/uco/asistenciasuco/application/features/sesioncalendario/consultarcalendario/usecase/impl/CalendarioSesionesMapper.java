package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/** ATA D7: traduce las filas del calendario (DATETIME2 en UTC sin zona) a instantes; lo comparten el estudiante y el docente. */
final class CalendarioSesionesMapper {

    private CalendarioSesionesMapper() {
    }

    static List<SesionCalendario> aSesiones(final List<SesionCalendarioRepositoryPort.SesionCalendario> filas) {
        return filas.stream().map(CalendarioSesionesMapper::aSesion).toList();
    }

    private static SesionCalendario aSesion(final SesionCalendarioRepositoryPort.SesionCalendario f) {
        return new SesionCalendario(f.idSesion(), f.idGrupo(), f.codigoGrupo(), f.nombreGrupo(), f.idAsignatura(), f.codigoAsignatura(),
                f.nombreAsignatura(), f.idDocente(), f.nombreDocente(), instante(f.fechaHoraInicioUtc()), instante(f.fechaHoraFinUtc()),
                f.esExtraordinaria(), f.estaCancelada(), f.motivoCancelacion(), f.esCancelacionAutomatica(), f.nombreDiaFestivo());
    }

    private static Instant instante(final LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
