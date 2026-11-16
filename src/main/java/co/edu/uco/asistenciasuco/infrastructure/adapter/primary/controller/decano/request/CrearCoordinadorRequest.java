package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NumeroIdentificacionTexto;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NombrePersona;
import java.util.UUID;
import jakarta.validation.Valid;

public final class CrearCoordinadorRequest {
    /** Tipo de documento elegido; si falta, la BD aplica el tipo por defecto del parametro IDENTIDAD. */
    private UUID tipoIdentificacionId;
    @NumeroIdentificacionTexto
    private String numeroIdentificacion;
    @NombrePersona
    private String primerNombre;
    @NombrePersona
    private String segundoNombre;
    @NombrePersona
    private String primerApellido;
    @NombrePersona
    private String segundoApellido;
    private String correo;
    private UUID idPrograma;
    @Valid
    private ProgramaNuevoRequest programaNuevo;
    public UUID getTipoIdentificacionId() { return tipoIdentificacionId; }
    public void setTipoIdentificacionId(final UUID tipoIdentificacionId) { this.tipoIdentificacionId = tipoIdentificacionId; }
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
    public UUID getIdPrograma() { return idPrograma; }
    public void setIdPrograma(final UUID idPrograma) { this.idPrograma = idPrograma; }
    public ProgramaNuevoRequest getProgramaNuevo() { return programaNuevo; }
    public void setProgramaNuevo(final ProgramaNuevoRequest programaNuevo) { this.programaNuevo = programaNuevo; }
}
