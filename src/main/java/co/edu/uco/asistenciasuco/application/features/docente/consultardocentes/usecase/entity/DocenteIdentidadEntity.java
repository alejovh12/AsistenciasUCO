package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity;

import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.helpers.TextHelper;

import java.util.UUID;

/**
 * Modelo interno para la identidad consultada de un docente.
 */
public final class DocenteIdentidadEntity {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UUID id;
    private final UUID idUsuario;
    private final Integer numeroIdentificacion;
    private final String nombreCompleto;
    private final boolean estaActivoUsuario;

    public DocenteIdentidadEntity(
            final UUID id,
            final UUID idUsuario,
            final Integer numeroIdentificacion,
            final String nombreCompleto,
            final boolean estaActivoUsuario
    ) {
        validarUsuario(idUsuario);
        validarNumeroIdentificacion(numeroIdentificacion);
        validarNombreCompleto(nombreCompleto);

        this.id = validarDocente(id);
        this.idUsuario = idUsuario;
        this.numeroIdentificacion = numeroIdentificacion;
        this.nombreCompleto = TextHelper.trim(nombreCompleto);
        this.estaActivoUsuario = estaActivoUsuario;
    }

    private UUID validarDocente(final UUID id) {
        if (ObjectHelper.isNull(id)) {
            throw new IllegalArgumentException("El docente es obligatorio.");
        }
        if (EMPTY_UUID.equals(id)) {
            throw new IllegalArgumentException("Debe seleccionar un docente valido.");
        }
        return id;
    }

    private void validarUsuario(final UUID idUsuario) {
        if (ObjectHelper.isNull(idUsuario)) {
            throw new IllegalArgumentException("El usuario del docente es obligatorio.");
        }
        if (EMPTY_UUID.equals(idUsuario)) {
            throw new IllegalArgumentException("El usuario del docente es valido.");
        }
    }

    private void validarNumeroIdentificacion(final Integer numeroIdentificacion) {
        if (ObjectHelper.isNull(numeroIdentificacion)) {
            throw new IllegalArgumentException("El numero de identificacion del docente es obligatorio.");
        }
    }

    private void validarNombreCompleto(final String nombreCompleto) {
        if (TextHelper.isNullOrBlank(nombreCompleto)) {
            throw new IllegalArgumentException("El nombre completo del docente es obligatorio.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public boolean isEstaActivoUsuario() {
        return estaActivoUsuario;
    }
}
