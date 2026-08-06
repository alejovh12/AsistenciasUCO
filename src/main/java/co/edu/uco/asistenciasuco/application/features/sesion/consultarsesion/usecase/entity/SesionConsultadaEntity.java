package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity;

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
        validarIdentificador(sesion, "La sesion es obligatoria.");
        validarIdentificador(grupo, "El grupo es obligatorio.");

        this.tema = validarTema(tema);
        this.descripcion = normalizarDescripcion(descripcion);
        this.observacionCierre = normalizarObservacionCierre(observacionCierre);
        this.cerrada = cerrada;
        this.sesion = sesion;
        this.grupo = grupo;
    }

    private void validarIdentificador(final UUID valor, final String mensaje) {
        if (ObjectHelper.isNull(valor)) {
            throw new IllegalArgumentException(mensaje);
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

    private String normalizarDescripcion(final String descripcion) {
        final String descripcionNormalizada = TextHelper.trim(descripcion);

        if (TextHelper.isNullOrBlank(descripcionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(descripcionNormalizada, 10, 250)) {
            throw new IllegalArgumentException("La descripcion de la sesion debe tener entre 10 y 250 caracteres.");
        }

        return descripcionNormalizada;
    }

    private String normalizarObservacionCierre(final String observacionCierre) {
        final String observacionNormalizada = TextHelper.trim(observacionCierre);

        if (TextHelper.isNullOrBlank(observacionNormalizada)) {
            return null;
        }

        if (!TextHelper.hasLengthBetween(observacionNormalizada, 5, 250)) {
            throw new IllegalArgumentException("La observacion de cierre debe tener entre 5 y 250 caracteres.");
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
