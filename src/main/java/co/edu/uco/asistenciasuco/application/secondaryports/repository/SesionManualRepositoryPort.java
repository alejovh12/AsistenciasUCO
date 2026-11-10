package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047) y DEC-065 (2): sesiones creadas manualmente por el docente y su solicitud de aval al coordinador. La DB decide que una sesion es manual
 * ({@code uv_sesion_manual}), calcula los destinatarios del aviso, valida el ambito del coordinador ({@code SEC_002}) y
 * aplica las reglas de cancelacion ({@code SES_011}, {@code SES_012}, {@code SES_016}, {@code SES_018}, {@code SES_019},
 * {@code SES_021}).
 */
public interface SesionManualRepositoryPort {

    /** {@code usp_consultar_destinatarios_sesion_manual}: estudiantes activos y coordinadores activos de los programas del grupo. */
    List<DestinatarioSesionManual> consultarDestinatarios(UUID sesionId, UUID docenteUsuarioId);

    /**
     * DEC-065 (2) {@code usp_consultar_destinatarios_solicitud_sesion}: DOCENTE titular y ESTUDIANTE activos de una sesion con
     * solicitud; lo ejecuta el coordinador (ambito {@code SEC_002}). Una sesion sin solicitud devuelve lista vacia.
     */
    List<DestinatarioSesionManual> consultarDestinatariosSolicitud(UUID sesionId, UUID coordinadorUsuarioId);

    /** {@code usp_aprobar_solicitud_sesion}: {@code SES_026} (exito), {@code SES_024} (ya resuelta), {@code SES_025} (sin solicitud). */
    SolicitudAprobada aprobarSolicitud(UUID sesionId, String observacion, UUID coordinadorUsuarioId);

    /** {@code usp_rechazar_solicitud_sesion}: la sesion queda cancelada con el motivo del catalogo ({@code SES_027}). */
    SolicitudRechazada rechazarSolicitud(UUID sesionId, String codigoMotivo, String observacion, UUID coordinadorUsuarioId);

    /** {@code usp_consultar_solicitudes_sesion_coordinador_paginado}: solicitudes de los programas del coordinador (una fila por sesion). */
    PaginaSolicitudesSesion consultarSolicitudes(UUID coordinadorUsuarioId, boolean soloPendientes, UUID programaId, UUID grupoId,
                                                 int pagina, int tamano);

    /** {@code usp_cancelar_sesion_coordinador}. */
    SesionCancelada cancelarPorCoordinador(UUID sesionId, String codigoMotivo, String observacion, UUID coordinadorUsuarioId);

    /** {@code usp_consultar_motivos_cancelacion_sesion}: catalogo de motivos activos, en su orden. */
    List<MotivoCancelacion> consultarMotivosCancelacion(UUID coordinadorUsuarioId);

    /** Destinatario del aviso: {@code tipo} es {@code ESTUDIANTE} o {@code COORDINADOR}. */
    record DestinatarioSesionManual(String tipo, UUID idUsuario, String nombreCompleto, String correo, String nombreAsignatura,
                                    String nombreGrupo, String nombreDocente, LocalDateTime fechaHoraInicio,
                                    LocalDateTime fechaHoraFin) {

        public static final String TIPO_ESTUDIANTE = "ESTUDIANTE";
        public static final String TIPO_COORDINADOR = "COORDINADOR";
        public static final String TIPO_DOCENTE = "DOCENTE";
    }

    /** Resultado de aprobar; {@code mensajeUsuario} es el del SP ({@code SES_026}). {@code fechaHoraInicio} es DATETIME2 almacenado. */
    record SolicitudAprobada(UUID idSesion, UUID idGrupo, LocalDateTime fechaHoraInicio, int totalEstudiantesActivos,
                             String mensajeUsuario) {
    }

    /** Resultado de rechazar; {@code mensajeUsuario} es el del SP ({@code SES_027}). */
    record SolicitudRechazada(UUID idSesion, UUID idGrupo, LocalDateTime fechaCancelacionUtc, int totalEstudiantesActivos,
                              String mensajeUsuario) {
    }

    /** Fila de {@code uv_solicitud_sesion}; las horas son DATETIME2 en UTC sin zona. */
    record SolicitudSesion(UUID idSesion, String nombreSesion, String codigoSesion, UUID idGrupo, String nombreGrupo,
                           String nombreAsignatura, UUID idPrograma, String nombrePrograma, UUID idDocente, String nombreDocente,
                           LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, String estadoSolicitud,
                           String nombreEstadoSolicitud, LocalDateTime fechaSolicitudUtc, LocalDateTime fechaResolucionUtc,
                           String observacion) {
    }

    record PaginaSolicitudesSesion(List<SolicitudSesion> elementos, long total) {
    }

    record MotivoCancelacion(String codigo, String nombre) {
    }
}
