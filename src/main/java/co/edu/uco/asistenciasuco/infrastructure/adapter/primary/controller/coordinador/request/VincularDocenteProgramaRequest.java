package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import java.util.UUID;

/** Cuerpo de {@code POST /api/v1/coordinador/docentes}. El programa NO viaja: sale del ambito del coordinador. */
public final class VincularDocenteProgramaRequest {
    private UUID idDocente;
    private UUID idUsuario;
    private UUID idTipoIdentificacion;
    private String numeroIdentificacion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String correo;
    private String password;
    private UUID idTipoVinculacion;

    public UUID getIdDocente() { return idDocente; }
    public void setIdDocente(final UUID idDocente) { this.idDocente = idDocente; }
    public UUID getIdUsuario() { return idUsuario; }
    public void setIdUsuario(final UUID idUsuario) { this.idUsuario = idUsuario; }
    public UUID getIdTipoIdentificacion() { return idTipoIdentificacion; }
    public void setIdTipoIdentificacion(final UUID idTipoIdentificacion) { this.idTipoIdentificacion = idTipoIdentificacion; }
    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(final String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
    public String getCorreo() { return correo; }
    public void setCorreo(final String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(final String password) { this.password = password; }
    public UUID getIdTipoVinculacion() { return idTipoVinculacion; }
    public void setIdTipoVinculacion(final UUID idTipoVinculacion) { this.idTipoVinculacion = idTipoVinculacion; }
}
