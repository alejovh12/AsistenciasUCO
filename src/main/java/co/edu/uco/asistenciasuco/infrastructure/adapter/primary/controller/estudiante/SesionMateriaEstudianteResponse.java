package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.TomaSesionEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Wire de {@code GET /api/v1/estudiante/materias/{materiaId}/sesiones}. Los campos existentes se conservan sin
 * cambios ({@code fechaHoraInicio}/{@code fechaHoraFin}: {@code LocalDateTime} sin zona). APP-04 agrega, de forma
 * aditiva y con la misma regla que HU060 coordinador, {@code fechaHoraInicioUtc}/{@code fechaHoraFinUtc} con sufijo
 * {@code Z} solo cuando {@code estadoTemporal = CONFIRMADA}; en otro caso son nulos y la SPA no inventa una zona.
 * CULM-001 B.1 (3.2.4): el {@code codigo} de la sesion deja de viajar al estudiante (ya no es una credencial de
 * asistencia, SUPUESTO-B-18) y se agregan {@code estado} y {@code cancelacion}; una sesion cancelada responde
 * {@code estadoAsistencia = NO_DISPONIBLE} y {@code reclamable = false}.
 * PC (puntos de control): agrega {@code minutosSesion}, {@code minutosAsistidos}, {@code minutosFalta}, {@code minutosExcusa},
 * {@code minutosSinRegistro} (nulos sin cabecera de asistencia) y {@code tomas}; {@code estadoAsistencia} sale de la base de datos.
 */
public record SesionMateriaEstudianteResponse(
        UUID id,
        String nombre,
        Integer numero,
        Integer numeroSemana,
        UUID idGrupo,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String fechaHoraInicioUtc,
        String fechaHoraFinUtc,
        String estadoTemporal,
        String estadoAsistencia,
        String estadoSolicitudRevision,
        boolean reclamable,
        String estado,
        CancelacionSesionResponse cancelacion,
        String fechaLimiteJustificacionUtc,
        boolean plazoJustificacionVigente,
        Integer minutosSesion,
        Integer minutosAsistidos,
        Integer minutosFalta,
        Integer minutosExcusa,
        Integer minutosSinRegistro,
        List<TomaSesionEstudianteResponse> tomas
) {

    public static SesionMateriaEstudianteResponse from(final SesionMateriaEstudianteDTO dto) {
        return new SesionMateriaEstudianteResponse(dto.id(), dto.nombre(), dto.numero(),
                dto.numeroSemana(), dto.idGrupo(), dto.codigoGrupo(), dto.nombreGrupo(), dto.fechaHoraInicio(),
                dto.fechaHoraFin(), utc(dto.fechaHoraInicioUtc()), utc(dto.fechaHoraFinUtc()), dto.estadoTemporal(),
                dto.estadoAsistencia(), dto.estadoSolicitudRevision(), dto.reclamable(), dto.estado(),
                CancelacionSesionResponse.from(dto.cancelacion()), utc(dto.fechaLimiteJustificacionUtc()),
                dto.plazoJustificacionVigente(), dto.minutosSesion(), dto.minutosAsistidos(), dto.minutosFalta(),
                dto.minutosExcusa(), dto.minutosSinRegistro(),
                dto.tomas().stream().map(TomaSesionEstudianteResponse::from).toList());
    }

    private static String utc(final LocalDateTime value) {
        return value == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(value);
    }

    /**
     * PC (puntos de control): toma de la sesion. {@code estado}: PRESENTE | AUSENTE | JUSTIFICADA | SIN_REGISTRO;
     * {@code estadoSolicitudRevision}: PENDIENTE | APROBADA | RECHAZADA o nulo; {@code resultadoAprobacion}: PRESENTE | JUSTIFICADA o nulo.
     */
    public record TomaSesionEstudianteResponse(UUID idToma, Integer numero, String fechaPuntoControlUtc, String estado,
                                               boolean reclamable, UUID idSolicitudRevision, String estadoSolicitudRevision,
                                               String resultadoAprobacion) {

        static TomaSesionEstudianteResponse from(final TomaSesionEstudianteDTO toma) {
            return new TomaSesionEstudianteResponse(toma.idToma(), toma.numero(),
                    toma.fechaPuntoControlUtc() == null ? null : toma.fechaPuntoControlUtc().toString(), toma.estado(),
                    toma.reclamable(), toma.idSolicitudRevision(), toma.estadoSolicitudRevision(), toma.resultadoAprobacion());
        }
    }
}
