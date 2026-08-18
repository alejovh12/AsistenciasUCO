package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.primaryports.dto;

import java.util.UUID;

/**
 * DTO publico para la identidad consultada de un docente.
 */
public final class DocenteIdentidadDTO {

    private UUID id;
    private UUID idUsuario;
    private Integer numeroIdentificacion;
    private String nombreCompleto;
    private boolean estaActivoUsuario;

    public DocenteIdentidadDTO() {
        super();
    }

    public DocenteIdentidadDTO(
            final UUID id,
            final UUID idUsuario,
            final Integer numeroIdentificacion,
            final String nombreCompleto,
            final boolean estaActivoUsuario
    ) {
        setId(id);
        setIdUsuario(idUsuario);
        setNumeroIdentificacion(numeroIdentificacion);
        setNombreCompleto(nombreCompleto);
        setEstaActivoUsuario(estaActivoUsuario);
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(final UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(final Integer numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(final String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public boolean isEstaActivoUsuario() {
        return estaActivoUsuario;
    }

    public void setEstaActivoUsuario(final boolean estaActivoUsuario) {
        this.estaActivoUsuario = estaActivoUsuario;
    }
}
