package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Modelo interno para la consulta de sesion.
 */
public final class SesionConsultadaEntity {

    private final UUID sesion;
    private final UUID grupo;
    private final String tema;
    private final String descripcion;
    private final boolean cerrada;
    private final String observacionCierre;

    public SesionConsultadaEntity(
            final UUID sesion,
            final UUID grupo,
            final String tema,
            final String descripcion,
            final boolean cerrada,
            final String observacionCierre
    ) {
        validarIdentificador(sesion, ErrorCode.ERR_SESION_REQUERIDA);
        validarIdentificador(grupo, ErrorCode.ERR_GRUPO_REQUERIDO);

        this.tema = validarTema(tema);
        this.descripcion = normalizarDescripcion(descripcion);
        this.observacionCierre = normalizarObservacionCierre(observacionCierre);
        this.cerrada = cerrada;
        this.sesion = sesion;
        this.grupo = grupo;
    }

    private void validarIdentificador(final UUID valor, final ErrorCode code) {
        if (ObjectHelper.isNull(valor)) {
            throw new ValidationException(code);
        }
    }

    private String validarTema(final String tema) {
        final String temaNormalizado = TextHelper.trim(tema);

        if (TextHelper.isNullOrBlank(temaNormalizado)) {
            throw new ValidationException(ErrorCode.ERR_TEMA_SESION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(temaNormalizado, 5, 100)) {
            throw new ValidationException(ErrorCode.ERR_TEMA_SESION_LONGITUD_INVALIDA);
        }

        return temaNormalizado;
    }

    private String normalizarDescripcion(final String descripcion) {
        final String descripcionNormalizada = TextHelper.trim(descripcion);

        if (TextHelper.isNullOrBlank(descripcionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(descripcionNormalizada, 10, 250)) {
            throw new ValidationException(ErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA);
        }

        return descripcionNormalizada;
    }

    private String normalizarObservacionCierre(final String observacionCierre) {
        final String observacionNormalizada = TextHelper.trim(observacionCierre);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new ValidationException(ErrorCode.ERR_OBSERVACION_CIERRE_LONGITUD_INVALIDA);
        }

        return observacionNormalizada;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public String getTema() {
        return tema;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isCerrada() {
        return cerrada;
    }

    public String getObservacionCierre() {
        return observacionCierre;
    }
}
