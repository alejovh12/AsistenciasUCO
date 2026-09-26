package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;



import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Dominio de la operacion crear sesion.
 */
public final class CrearSesionDomain {

    private final UUID grupo;
    private final String nombre;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final UUID usuarioEjecutor;

    public CrearSesionDomain(
            final UUID grupo,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor
    ) {
        validarGrupo(grupo);
        validarUsuarioEjecutor(usuarioEjecutor);
        validarFechas(fechaHoraInicio, fechaHoraFin);
        this.nombre = validarNombre(nombre);
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.usuarioEjecutor = usuarioEjecutor;

        this.grupo = grupo;
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
    }

    private void validarUsuarioEjecutor(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    private void validarFechas(final LocalDateTime fechaHoraInicio, final LocalDateTime fechaHoraFin) {
        if (fechaHoraInicio == null || fechaHoraFin == null || !fechaHoraFin.isAfter(fechaHoraInicio)) {
            throw new ValidationException(SesionErrorCode.ERR_RANGO_FECHAS_SESION_INVALIDO);
        }
    }

    private String validarNombre(final String nombre) {
        final String normalizado = TextHelper.trim(nombre);

        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(normalizado, 1, 50)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_LONGITUD_INVALIDA);
        }

        return normalizado;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

}
