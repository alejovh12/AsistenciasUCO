package co.edu.uco.asistenciasuco.application.features.sesion.common.dto;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * CULM-001 B.1 (§3.2.4): cancelacion de una sesion tal como la ven todos los roles. {@code null} en una sesion
 * programada.
 *
 * @param tipo                codigo del tipo de cancelacion ({@code DOCENTE}, {@code FESTIVO} o, DEC-081, {@code RECESO}; catalogo de la DB)
 * @param nombreFestivo       nombre del festivo cuando la cancelo el sistema por festivo; {@code null} si la cancelo el docente o un receso academico (su motivo trae el prefijo «Receso academico: »)
 * @param fechaCancelacionUtc instante UTC de la cancelacion
 */
public record CancelacionSesionDTO(String tipo, String motivo, String nombreFestivo, Instant fechaCancelacionUtc) {

    /** Traduce la fila de {@code uv_sesion_estado}; {@code null} si la sesion no esta cancelada. */
    public static CancelacionSesionDTO desde(final CancelacionSesionProjection projection) {
        if (projection == null) {
            return null;
        }
        return new CancelacionSesionDTO(
                projection.codigoTipo(),
                projection.motivo(),
                projection.nombreFestivo(),
                projection.fechaCancelacionUtc() == null ? null : projection.fechaCancelacionUtc().toInstant(ZoneOffset.UTC));
    }
}
