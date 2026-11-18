package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ATA D7: calendario de sesiones (horario semanal con fechas) de un estudiante o de un docente titular, sobre
 * {@code usp_consultar_sesiones_calendario_estudiante} y {@code usp_consultar_sesiones_calendario_docente}. La DB valida el perfil
 * ({@code SEC_001}) y el rango en fecha LOCAL institucional ({@code CAL_001}: ambas fechas, hasta &gt;= desde, maximo 31 dias).
 */
public interface SesionCalendarioRepositoryPort {

    List<SesionCalendario> consultarEstudiante(UUID usuarioId, LocalDate desde, LocalDate hasta);

    List<SesionCalendario> consultarDocente(UUID usuarioId, LocalDate desde, LocalDate hasta);

    /** Fila de {@code uv_sesion_calendario}; las horas son DATETIME2 en UTC sin zona. */
    record SesionCalendario(UUID idSesion, UUID idGrupo, Integer codigoGrupo, String nombreGrupo, UUID idAsignatura,
                            String codigoAsignatura, String nombreAsignatura, UUID idDocente, String nombreDocente,
                            LocalDateTime fechaHoraInicioUtc, LocalDateTime fechaHoraFinUtc, boolean esExtraordinaria,
                            boolean estaCancelada, String motivoCancelacion, boolean esCancelacionAutomatica,
                            String nombreDiaFestivo) {
    }
}
