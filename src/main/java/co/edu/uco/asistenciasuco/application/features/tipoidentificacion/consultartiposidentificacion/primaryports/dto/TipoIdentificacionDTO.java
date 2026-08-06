package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto;

import java.util.UUID;

/**
 * DTO de salida para un tipo de identificacion consultado.
 */
public final class TipoIdentificacionDTO {

    private UUID id;
    private String tipoIdentificacion;
    private String nombre;

    public TipoIdentificacionDTO() {
        super();
    }

    public TipoIdentificacionDTO(
            final UUID id,
            final String tipoIdentificacion,
            final String nombre
    ) {
        setId(id);
        setTipoIdentificacion(tipoIdentificacion);
        setNombre(nombre);
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(final String tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }
}
