package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain;

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
            throw new IllegalArgumentException("El grupo es obligatorio.");
        }
    }

    private String validarTema(final String tema) {
        final String temaNormalizado = TextHelper.trim(tema);

        if (TextHelper.isNullOrBlank(temaNormalizado)) {
            throw new IllegalArgumentException("El tema de la sesion es obligatorio.");
        }

        if (!TextHelper.hasLengthBetween(temaNormalizado, 5, 100)) {
            throw new IllegalArgumentException("El tema de la sesion debe tener entre 5 y 100 caracteres.");
        }

        return temaNormalizado;
    }

    private String validarDescripcion(final String descripcion) {
        final String descripcionNormalizada = TextHelper.trim(descripcion);

        if (TextHelper.isNullOrBlank(descripcionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(descripcionNormalizada, 10, 250)) {
            throw new IllegalArgumentException("La descripcion de la sesion debe tener entre 10 y 250 caracteres.");
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
