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
    private final UUID idCorrelacion;

    public CrearSesionDomain(
            final UUID grupo,
            final String tema,
            final String descripcion,
            final UUID idCorrelacion
    ) {
        validarGrupo(grupo);
        this.tema = validarTema(tema);
        this.descripcion = validarDescripcion(descripcion);
        validarIdCorrelacion(idCorrelacion);

        this.grupo = grupo;
        this.idCorrelacion = idCorrelacion;
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

    private void validarIdCorrelacion(final UUID idCorrelacion) {
        if (ObjectHelper.isNull(idCorrelacion)) {
            throw new IllegalArgumentException("El id de correlacion es obligatorio.");
        }
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

    public UUID getIdCorrelacion() {
        return idCorrelacion;
    }
}
