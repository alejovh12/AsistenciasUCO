package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fila de {@code uv_sesion_v2}: las diez columnas de la vista v1 mas la procedencia temporal y (CULM-001 B.1) la
 * cancelacion leida de {@code uv_sesion_estado} y (DEC-065 (2)) el estado de la solicitud de sesion extraordinaria.
 *
 * @param fechaHoraInicio valor DATETIME2 almacenado, sin interpretar
 * @param procedenciaTemporal NULL cuando la fila es historica o fue escrita por v1
 * @param cancelacion {@code null} si la sesion esta programada
 * @param estadoSolicitud {@code PENDIENTE|APROBADA|RECHAZADA}; {@code null} si la sesion no nacio como solicitud
 */
public record SesionProcedenciaRepositoryProjection(
        UUID sesion,
        UUID grupo,
        String nombre,
        Integer numero,
        String codigo,
        Integer numeroSemana,
        String codigoGrupo,
        String nombreGrupo,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        String procedenciaTemporal,
        CancelacionSesionProjection cancelacion,
        String estadoSolicitud,
        boolean pendienteAprobacion
) {

    /** Sesion con cancelacion opcional y sin solicitud (DEC-065 (2): {@code estadoSolicitud} nulo). */
    public SesionProcedenciaRepositoryProjection(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String procedenciaTemporal,
            final CancelacionSesionProjection cancelacion
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                procedenciaTemporal, cancelacion, null, false);
    }

    /** Sesion programada (sin cancelacion). */
    public SesionProcedenciaRepositoryProjection(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final String procedenciaTemporal
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin,
                procedenciaTemporal, null);
    }
}
