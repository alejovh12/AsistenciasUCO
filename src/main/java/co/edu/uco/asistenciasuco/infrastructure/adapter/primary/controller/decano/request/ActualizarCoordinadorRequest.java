package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NombrePersona;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorreccionDocumentoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.BeanValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/decano/coordinadores/{id}} (HU121). HU159 (2026-11-05): el tipo y el numero de
 * documento son opcionales (ausentes = no cambian) y, si cambian, exigen motivo.
 */
public final class ActualizarCoordinadorRequest extends CorreccionDocumentoRequest {
    @NotBlank(message = "El primer nombre es obligatorio.")
    @Size(max = 50, message = "El campo primerNombre no puede superar 50 caracteres.")
    @NombrePersona
    private String primerNombre;
    @Size(max = 50, message = "El campo segundoNombre no puede superar 50 caracteres.")
    @NombrePersona
    private String segundoNombre;
    @NotBlank(message = "El primer apellido es obligatorio.")
    @Size(max = 50, message = "El campo primerApellido no puede superar 50 caracteres.")
    @NombrePersona
    private String primerApellido;
    @Size(max = 50, message = "El campo segundoApellido no puede superar 50 caracteres.")
    @NombrePersona
    private String segundoApellido;
    @NotBlank(message = "El correo electronico es obligatorio.")
    @Pattern(regexp = BeanValidationPatterns.EMAIL_OR_BLANK, message = "El correo electronico no tiene un formato valido.")
    @Size(max = 100, message = "El correo electronico no puede superar 100 caracteres.")
    private String correo;

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
}
