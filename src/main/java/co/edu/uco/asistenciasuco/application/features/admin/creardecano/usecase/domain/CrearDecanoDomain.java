package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion crear decano.
 */
public final class CrearDecanoDomain {

    private final UUID tipoIdentificacionId;
    private final Integer numeroIdentificacion;
    private final String primerNombre;
    private final String segundoNombre;
    private final String primerApellido;
    private final String segundoApellido;
    private final String correo;
    private final String password;
    private final UUID idFacultad;

    public CrearDecanoDomain(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final String password,
            final UUID idFacultad
    ) {
        this.tipoIdentificacionId = tipoIdentificacionId;
        this.numeroIdentificacion = numeroIdentificacion;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.correo = correo;
        this.password = password;
        this.idFacultad = idFacultad;
    }

    public UUID getTipoIdentificacionId() {
        return tipoIdentificacionId;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getPassword() {
        return password;
    }

    public UUID getIdFacultad() {
        return idFacultad;
    }
}
