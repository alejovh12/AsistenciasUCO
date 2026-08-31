package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * Proyeccion del puerto secundario para resultados de tipo de identificacion.
 */
public final class TipoIdentificacionRepositoryProjection {

    private final UUID id;
    private final String tipoIdentificacion;
    private final String nombre;

    public TipoIdentificacionRepositoryProjection(
            final UUID id,
            final String tipoIdentificacion,
            final String nombre
    ) {
        this.id = id;
        this.tipoIdentificacion = tipoIdentificacion;
        this.nombre = nombre;
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
