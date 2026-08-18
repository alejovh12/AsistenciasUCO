package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain;

import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * Dominio del catalogo de tipos de identificacion.
 */
public final class TipoIdentificacionDomain {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);
    private static final int MAX_LENGTH_CODIGO = 5;
    private static final int MAX_LENGTH_NOMBRE = 50;

    private final UUID id;
    private final String tipoIdentificacion;
    private final String nombre;

    private TipoIdentificacionDomain(
            final UUID id,
            final String tipoIdentificacion,
            final String nombre
    ) {
        this.id = id;
        this.tipoIdentificacion = tipoIdentificacion;
        this.nombre = nombre;
    }

    public static TipoIdentificacionDomain reconstruir(
            final UUID id,
            final String tipoIdentificacion,
            final String nombre
    ) {
        return new TipoIdentificacionDomain(
                validarId(id),
                validarCodigo(tipoIdentificacion),
                validarNombre(nombre)
        );
    }

    private static UUID validarId(final UUID id) {
        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("El tipo de identificacion es obligatorio.");
        }
        if (EMPTY_UUID.equals(id)) {
            throw new IllegalArgumentException("Debe seleccionar un tipo de identificacion valido.");
        }
        return id;
    }

    private static String validarCodigo(final String tipoIdentificacion) {
        final String normalizado = TextHelper.trim(tipoIdentificacion);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new IllegalArgumentException("El codigo del tipo de identificacion es obligatorio.");
        }
        if (normalizado.length() > MAX_LENGTH_CODIGO) {
            throw new IllegalArgumentException("El codigo del tipo de identificacion debe tener maximo 5 caracteres.");
        }
        return normalizado;
    }

    private static String validarNombre(final String nombre) {
        final String normalizado = TextHelper.trim(nombre);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new IllegalArgumentException("El nombre del tipo de identificacion es obligatorio.");
        }
        if (normalizado.length() > MAX_LENGTH_NOMBRE) {
            throw new IllegalArgumentException("El nombre del tipo de identificacion debe tener maximo 50 caracteres.");
        }
        return normalizado;
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

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TipoIdentificacionDomain other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
