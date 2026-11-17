package co.edu.uco.asistenciasuco.application.features.docente.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Resumen del grupo con sus sesiones (orden por numero). {@code promedioAsistencia} es {@code null} mientras no haya horas
 * dictadas; {@code idSesionEnCurso} e {@code idSesionProxima} son {@code null} si no hay sesion en curso o proxima.
 */
public record ResumenSesionesGrupoDTO(int sesionesProgramadas, int sesionesEjecutadas, int sesionesCanceladas, int sesionesEnCurso,
                                      UUID idSesionEnCurso, UUID idSesionProxima, int estudiantesActivos, int estudiantesEnRiesgo,
                                      BigDecimal promedioAsistencia, BigDecimal umbralAdvertencia, BigDecimal umbralPerdida,
                                      List<Sesion> sesiones) {

    public ResumenSesionesGrupoDTO {
        sesiones = sesiones == null ? List.of() : List.copyOf(sesiones);
    }

    /** {@code estadoSesion}: PROGRAMADA, EN_CURSO, EJECUTADA o CANCELADA; los conteos son sobre las matriculas activas. */
    public record Sesion(UUID idSesion, int numero, String nombre, Instant fechaHoraInicioUtc, Instant fechaHoraFinUtc,
                         boolean horarioConfirmado, String estadoSesion, String estadoSolicitud, boolean pendienteAprobacion,
                         int totalEstudiantes, int presentes, int parciales, int justificadas, int ausentes, int sinRegistro) {
    }
}
