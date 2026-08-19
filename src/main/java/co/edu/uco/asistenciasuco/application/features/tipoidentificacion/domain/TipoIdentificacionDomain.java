package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain;

import co.edu.uco.asistenciasuco.application.exception.ErrorCode;
import co.edu.uco.asistenciasuco.application.exception.ValidationException;
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
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_REQUERIDA);
        }
        if (EMPTY_UUID.equals(id)) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_INVALIDA);
        }
        return id;
    }

    private static String validarCodigo(final String tipoIdentificacion) {
        final String normalizado = TextHelper.trim(tipoIdentificacion);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_CODIGO_REQUERIDO);
        }
        if (normalizado.length() > MAX_LENGTH_CODIGO) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_CODIGO_LONGITUD_INVALIDA);
        }
        return normalizado;
    }

    private static String validarNombre(final String nombre) {
        final String normalizado = TextHelper.trim(nombre);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_NOMBRE_REQUERIDO);
        }
        if (normalizado.length() > MAX_LENGTH_NOMBRE) {
            throw new ValidationException(ErrorCode.ERR_TIPO_IDENTIFICACION_NOMBRE_LONGITUD_INVALIDA);
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
