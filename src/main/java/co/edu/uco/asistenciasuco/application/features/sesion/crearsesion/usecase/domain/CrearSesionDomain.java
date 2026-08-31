package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;



import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Dominio de la operacion crear sesion.
 */
public final class CrearSesionDomain {

    private final UUID grupo;
    private final String tema;
    private final String descripcion;

    public CrearSesionDomain(
            final UUID grupo,
            final String tema,
            final String descripcion
    ) {
        validarGrupo(grupo);
        this.tema = validarTema(tema);
        this.descripcion = validarDescripcion(descripcion);

        this.grupo = grupo;
    }

    private void validarGrupo(final UUID grupo) {
        if (ObjectHelper.isNull(grupo)) {
            throw new ValidationException(GrupoErrorCode.ERR_GRUPO_REQUERIDO);
        }
    }

    private String validarTema(final String tema) {
        final String temaNormalizado = TextHelper.trim(tema);

        if (TextHelper.isNullOrBlank(temaNormalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(temaNormalizado, 5, 100)) {
            throw new ValidationException(SesionErrorCode.ERR_TEMA_SESION_LONGITUD_INVALIDA);
        }

        return temaNormalizado;
    }

    private String validarDescripcion(final String descripcion) {
        final String descripcionNormalizada = TextHelper.trim(descripcion);

        if (TextHelper.isNullOrBlank(descripcionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(descripcionNormalizada, 10, 250)) {
            throw new ValidationException(SesionErrorCode.ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA);
        }

        return descripcionNormalizada;
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

}
