package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NombrePersona;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorreccionDocumentoRequest;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/v1/coordinador/docentes/{idDocente}}. Los campos ausentes no se modifican; el correo no se
 * edita. HU159 (2026-11-05): el tipo y el numero de documento son opcionales y, si cambian, exigen motivo.
 */
public final class ActualizarDocenteProgramaRequest extends CorreccionDocumentoRequest {
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

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
}
