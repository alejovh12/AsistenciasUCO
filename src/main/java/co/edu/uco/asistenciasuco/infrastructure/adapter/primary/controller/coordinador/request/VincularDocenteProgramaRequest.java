package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NumeroIdentificacionTexto;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NombrePersona;
import java.util.UUID;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

/** Cuerpo de {@code POST /api/v1/coordinador/docentes}. El programa NO viaja: sale del ambito del coordinador. */
public final class VincularDocenteProgramaRequest {
    @NonEmptyUuid(message = "El identificador idDocente no es valido.")
    private UUID idDocente;
    @NonEmptyUuid(message = "El identificador idUsuario no es valido.")
    private UUID idUsuario;
    @NonEmptyUuid(message = "El identificador idTipoIdentificacion no es valido.")
    private UUID idTipoIdentificacion;
    @Pattern(regexp = BeanValidationPatterns.DIGITS_OR_BLANK, message = "El numero de identificacion debe ser numerico.")
    @NumeroIdentificacionTexto
    private String numeroIdentificacion;
    @Size(max = 50, message = "El campo primerNombre excede la longitud permitida.")
    @NombrePersona
    private String primerNombre;
    @Size(max = 50, message = "El campo segundoNombre excede la longitud permitida.")
    @NombrePersona
    private String segundoNombre;
    @Size(max = 50, message = "El campo primerApellido excede la longitud permitida.")
    @NombrePersona
    private String primerApellido;
    @Size(max = 50, message = "El campo segundoApellido excede la longitud permitida.")
    @NombrePersona
    private String segundoApellido;
    @Size(max = 100, message = "El correo excede la longitud permitida.")
    private String correo;
    private String password;
    @NonEmptyUuid(message = "El identificador idTipoVinculacion no es valido.")
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
