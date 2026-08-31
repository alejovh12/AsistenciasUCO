package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * Proyeccion del puerto secundario para la identidad consultada de un docente.
 */
public final class DocenteIdentidadRepositoryProjection {

    private final UUID id;
    private final UUID idUsuario;
    private final Integer numeroIdentificacion;
    private final String nombreCompleto;
    private final boolean estaActivoUsuario;

    public DocenteIdentidadRepositoryProjection(
            final UUID id,
            final UUID idUsuario,
            final Integer numeroIdentificacion,
            final String nombreCompleto,
            final boolean estaActivoUsuario
    ) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.numeroIdentificacion = numeroIdentificacion;
        this.nombreCompleto = nombreCompleto;
        this.estaActivoUsuario = estaActivoUsuario;
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
