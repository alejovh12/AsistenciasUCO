package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code estadoAsistencia}: PRESENTE | AUSENTE | JUSTIFICADA | PARCIAL | NO_DISPONIBLE (nunca nulo). PARCIAL
 * (CULM-001 B.2 [SUPUESTO-B-15], CERT-CULM001): varias tomas en la sesion, alguna {@code SJC} y otra {@code AN}.
 * {@code estadoSolicitudRevision}: nombre del estado de la solicitud de revision existente, o nulo.
 * {@code reclamable}: pista de UI (ausencia confirmada, total o PARCIAL, y sin solicitud previa); no sustituye la
 * validacion al radicar.
 * CULM-001 D.1: {@code fechaLimiteJustificacionUtc} (limite exclusivo en UTC, nulo si faltan los parametros) y
 * {@code plazoJustificacionVigente} salen de la base de datos; el cliente no calcula el plazo.
 * PC (puntos de control): {@code estadoAsistencia} sale de la base de datos ({@code codigoEstadoSesionEstudiante}); {@code minutos*}
 * y {@code tomas} (una por punto de control, con su estado y si es reclamable) tambien.
 */
public record SesionMateriaEstudianteDTO(UUID id, String nombre, Integer numero, String codigo, Integer numeroSemana,
                                         UUID idGrupo, String codigoGrupo, String nombreGrupo,
                                         LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin,
                                         LocalDateTime fechaHoraInicioUtc, LocalDateTime fechaHoraFinUtc,
                                         String estadoTemporal,
                                         String estadoAsistencia, String estadoSolicitudRevision, boolean reclamable,
                                         String estado, CancelacionSesionDTO cancelacion,
                                         LocalDateTime fechaLimiteJustificacionUtc,
                                         boolean plazoJustificacionVigente,
                                         Integer minutosSesion, Integer minutosAsistidos, Integer minutosFalta,
                                         Integer minutosExcusa, Integer minutosSinRegistro,
                                         List<TomaSesionEstudianteDTO> tomas) {

    public SesionMateriaEstudianteDTO {
        tomas = tomas == null ? List.of() : List.copyOf(tomas);
    }

    /** Sesion con plazo y sin minutos ni tomas (compatibilidad). */
    public SesionMateriaEstudianteDTO(final UUID id, final String nombre, final Integer numero, final String codigo,
                                      final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                      final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                      final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                      final LocalDateTime fechaHoraFinUtc, final String estadoTemporal,
                                      final String estadoAsistencia, final String estadoSolicitudRevision,
                                      final boolean reclamable, final String estado, final CancelacionSesionDTO cancelacion,
                                      final LocalDateTime fechaLimiteJustificacionUtc, final boolean plazoJustificacionVigente) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                estado, cancelacion, fechaLimiteJustificacionUtc, plazoJustificacionVigente, null, null, null, null, null, null);
    }

    /** Sesion con estado y cancelacion, sin informacion de plazo. */
    public SesionMateriaEstudianteDTO(final UUID id, final String nombre, final Integer numero, final String codigo,
                                      final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                      final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                      final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                      final LocalDateTime fechaHoraFinUtc, final String estadoTemporal,
                                      final String estadoAsistencia, final String estadoSolicitudRevision,
                                      final boolean reclamable, final String estado, final CancelacionSesionDTO cancelacion) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                estado, cancelacion, null, false);
    }

    /** Sesion programada (sin cancelacion). */
    public SesionMateriaEstudianteDTO(final UUID id, final String nombre, final Integer numero, final String codigo,
                                      final Integer numeroSemana, final UUID idGrupo, final String codigoGrupo,
                                      final String nombreGrupo, final LocalDateTime fechaHoraInicio,
                                      final LocalDateTime fechaHoraFin, final LocalDateTime fechaHoraInicioUtc,
                                      final LocalDateTime fechaHoraFinUtc, final String estadoTemporal,
                                      final String estadoAsistencia, final String estadoSolicitudRevision,
                                      final boolean reclamable) {
        this(id, nombre, numero, codigo, numeroSemana, idGrupo, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, estadoAsistencia, estadoSolicitudRevision, reclamable,
                EstadoSesion.PROGRAMADA.name(), null, null, false);
    }
}
