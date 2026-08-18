package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request HTTP para registrar estudiante en un grupo.
 */
public final class RegistrarEstudianteRequest {

    @NotNull
    private UUID tipoIdentificacionId;

    @NotNull
    @Positive
    private Integer numeroIdentificacion;

    @NotBlank
    @Size(max = 50)
    private String primerApellido;

    @Size(max = 50)
    private String segundoApellido;

    @NotBlank
    @Size(max = 50)
    private String primerNombre;

    @Size(max = 50)
    private String segundoNombre;

    @NotBlank
    @Email
    @Size(max = 100)
    private String correo;

    @NotBlank
    @Size(max = 255)
    private String password;

    public UUID getTipoIdentificacionId() {
        return tipoIdentificacionId;
    }

    public void setTipoIdentificacionId(final UUID tipoIdentificacionId) {
        this.tipoIdentificacionId = tipoIdentificacionId;
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
