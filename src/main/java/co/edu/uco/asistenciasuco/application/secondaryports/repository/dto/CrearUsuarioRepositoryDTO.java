package co.edu.uco.asistenciasuco.application.secondaryports.repository.dto;

import java.util.UUID;

/**
 * DTO del puerto secundario para crear un usuario base.
 */
public final class CrearUsuarioRepositoryDTO {

    private UUID tipoIdIdentificacion;
    private Integer numeroIdentificacion;
    private String primerApellido;
    private String segundoApellido;
    private String primerNombre;
    private String segundoNombre;
    private String correo;
    private String password;

    public CrearUsuarioRepositoryDTO() {
        super();
    }

    public CrearUsuarioRepositoryDTO(
            final UUID tipoIdIdentificacion,
            final Integer numeroIdentificacion,
            final String primerApellido,
            final String segundoApellido,
            final String primerNombre,
            final String segundoNombre,
            final String correo,
            final String password
    ) {
        setTipoIdIdentificacion(tipoIdIdentificacion);
        setNumeroIdentificacion(numeroIdentificacion);
        setPrimerApellido(primerApellido);
        setSegundoApellido(segundoApellido);
        setPrimerNombre(primerNombre);
        setSegundoNombre(segundoNombre);
        setCorreo(correo);
        setPassword(password);
    }

    public UUID getTipoIdIdentificacion() {
        return tipoIdIdentificacion;
    }

    public void setTipoIdIdentificacion(final UUID tipoIdIdentificacion) {
        this.tipoIdIdentificacion = tipoIdIdentificacion;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public void setNumeroIdentificacion(final Integer numeroIdentificacion) {
        this.numeroIdentificacion = numeroIdentificacion;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(final String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(final String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(final String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(final String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(final String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
