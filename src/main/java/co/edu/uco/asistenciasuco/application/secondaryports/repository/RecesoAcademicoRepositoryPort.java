package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DEC-081: receso academico del periodo (dias completos sin clase para toda la institucion). Puerto de salida sobre
 * {@code usp_crear_receso_academico}, {@code usp_retirar_receso_academico}, {@code usp_consultar_recesos_periodo} y
 * {@code usp_consultar_destinatarios_receso_academico}. Las reglas (fecha dentro del periodo y futura, no festivo, no repetida, periodo
 * abierto, dia ya aplicado) y el ambito (solo el ADMINISTRADOR de la institucion del periodo escribe) las decide la DB; un rechazo sale
 * como excepcion semantica con su codigo {@code REC_*}.
 */
public interface RecesoAcademicoRepositoryPort {

    /**
     * Registra el dia y cancela al instante las sesiones futuras de ese dia en los grupos del periodo. {@code idReceso} lo genera el
     * backend. El detalle por sesion NO se pide a la DB aqui ({@code @incluirDetalle = 0}): el caso de uso lo obtiene de
     * {@link #consultarDestinatarios}, que trae una fila por persona y sesion cancelada (con su grupo y su hora de inicio).
     */
    RecesoCreado crear(UUID idReceso, UUID idPeriodo, LocalDate fecha, String motivo, UUID usuarioEjecutor);

    /** Retiro logico; devuelve el {@code mensajeUsuario} del SP (REC_002). Si el dia ya cancelo sesiones: {@code REC_010} (409). */
    String retirar(UUID idReceso, UUID usuarioEjecutor);

    /** Dias activos del periodo ordenados por fecha. Lectura para ADMINISTRADOR, DECANO, COORDINADOR y DOCENTE de la institucion. */
    List<RecesoPeriodo> consultar(UUID idPeriodo, UUID usuarioEjecutor);

    /** Una fila por (persona, sesion cancelada); solo el ADMINISTRADOR. El caso de uso agrupa por persona. */
    List<DestinatarioReceso> consultarDestinatarios(UUID idReceso, UUID usuarioEjecutor);

    /** {@code mensajeUsuario} es el REC_001 que sustituyo la DB ({fecha} y {n} clases canceladas). */
    record RecesoCreado(UUID idReceso, int sesionesCanceladas, int sesionesConAsistencia, String mensajeUsuario) {
    }

    /** Fila de {@code usp_consultar_recesos_periodo}. */
    record RecesoPeriodo(UUID id, UUID idPeriodoAcademico, LocalDate fecha, String motivo, int sesionesCanceladas,
                         boolean estaAplicado, boolean puedeRetirarse, LocalDateTime fechaCreacionUtc) {
    }

    /**
     * Fila de {@code usp_consultar_destinatarios_receso_academico}. {@code rolDestinatario} es {@code ESTUDIANTE} o {@code DOCENTE};
     * las horas son UTC (DATETIME2 sin zona); {@code motivoCancelacion} ya lleva el prefijo institucional.
     */
    record DestinatarioReceso(String rolDestinatario, UUID idUsuario, String nombreCompleto, String correo, UUID idSesion, UUID idGrupo,
                              String nombreAsignatura, String nombreGrupo, LocalDate fecha, LocalDateTime fechaHoraInicio,
                              LocalDateTime fechaHoraFin, String motivoCancelacion) {

        public static final String ROL_ESTUDIANTE = "ESTUDIANTE";
        public static final String ROL_DOCENTE = "DOCENTE";
    }
}
