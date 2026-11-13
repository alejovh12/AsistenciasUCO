package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de la correccion de identidad (HU159) de un estudiante o de un decano: nombres y apellidos, documento
 * opcional (tipo y numero juntos) y motivo obligatorio.
 */
public final class CorregirIdentidadRequest extends CorreccionDocumentoRequest {
    @NotBlank(message = "El primer nombre es obligatorio.")
    @Size(max = 50, message = "El primer nombre no puede superar 50 caracteres.")
    private String primerNombre;
    @Size(max = 50, message = "El segundo nombre no puede superar 50 caracteres.")
    private String segundoNombre;
    @NotBlank(message = "El primer apellido es obligatorio.")
    @Size(max = 50, message = "El primer apellido no puede superar 50 caracteres.")
    private String primerApellido;
    @Size(max = 50, message = "El segundo apellido no puede superar 50 caracteres.")
    private String segundoApellido;

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
}
