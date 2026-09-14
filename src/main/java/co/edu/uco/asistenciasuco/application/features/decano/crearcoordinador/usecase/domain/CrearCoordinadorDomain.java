package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion crear coordinador.
 */
public final class CrearCoordinadorDomain {

    private final String numeroIdentificacion;
    private final String primerNombre;
    private final String segundoNombre;
    private final String primerApellido;
    private final String segundoApellido;
    private final String correo;
    private final UUID idPrograma;
    private final String password;
    private final UUID usuario;

    public CrearCoordinadorDomain(
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final String password,
            final UUID usuario
    ) {
        this.numeroIdentificacion = numeroIdentificacion;
        this.primerNombre = primerNombre;
        this.segundoNombre = segundoNombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.correo = correo;
        this.idPrograma = idPrograma;
        this.password = password;
        this.usuario = usuario;
    }

    public String getNumeroIdentificacion() {
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

    public UUID getIdPrograma() {
        return idPrograma;
    }

    public String getPassword() {
        return password;
    }

    public UUID getUsuario() {
        return usuario;
    }
}
