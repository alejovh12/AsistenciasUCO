package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ATA D7: modelo comun del calendario de sesiones (horario semanal con fechas) del estudiante y del docente. {@code desde} y
 * {@code hasta} son fechas LOCALES institucionales (ambas inclusivas); las horas del resultado son instantes UTC.
 */
public interface CalendarioSesiones {

    record Comando(UUID usuarioId, LocalDate desde, LocalDate hasta) {
    }

    record SesionCalendario(UUID idSesion, UUID idGrupo, Integer codigoGrupo, String nombreGrupo, UUID idAsignatura,
                            String codigoAsignatura, String nombreAsignatura, UUID idDocente, String nombreDocente,
                            Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc, boolean esExtraordinaria, boolean estaCancelada,
                            String motivoCancelacion, boolean esCancelacionAutomatica, String nombreDiaFestivo) {
    }
}
