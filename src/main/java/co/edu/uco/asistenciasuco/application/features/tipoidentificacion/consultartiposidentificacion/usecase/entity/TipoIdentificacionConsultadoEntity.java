package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.entity;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Modelo interno para la consulta de tipos de identificacion.
 */
public final class TipoIdentificacionConsultadoEntity {

    private final UUID id;
    private final String tipoIdentificacion;
    private final String nombre;

    public TipoIdentificacionConsultadoEntity(
            final UUID id,
            final String tipoIdentificacion,
            final String nombre
    ) {
        this.id = validarId(id);
        this.tipoIdentificacion = validarTipoIdentificacion(tipoIdentificacion);
        this.nombre = validarNombre(nombre);
    }

    private UUID validarId(final UUID id) {
        if (ObjectHelper.isNull(id)) {
            throw new IllegalArgumentException("El id del tipo de identificacion es obligatorio.");
        }
        return id;
    }

    private String validarTipoIdentificacion(final String tipoIdentificacion) {
        final String valorNormalizado = TextHelper.trim(tipoIdentificacion);

        if (TextHelper.isNullOrBlank(valorNormalizado)) {
            throw new IllegalArgumentException("El codigo del tipo de identificacion es obligatorio.");
        }

        if (!TextHelper.hasLengthBetween(valorNormalizado, 1, 5)) {
            throw new IllegalArgumentException("El codigo del tipo de identificacion debe tener maximo 5 caracteres.");
        }

        return valorNormalizado;
    }

    private String validarNombre(final String nombre) {
        final String valorNormalizado = TextHelper.trim(nombre);

        if (TextHelper.isNullOrBlank(valorNormalizado)) {
            throw new IllegalArgumentException("El nombre del tipo de identificacion es obligatorio.");
        }

        if (!TextHelper.hasLengthBetween(valorNormalizado, 1, 50)) {
            throw new IllegalArgumentException("El nombre del tipo de identificacion debe tener maximo 50 caracteres.");
        }

        return valorNormalizado;
    }

    public UUID getId() {
        return id;
    }

    public String getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public String getNombre() {
        return nombre;
    }
}
