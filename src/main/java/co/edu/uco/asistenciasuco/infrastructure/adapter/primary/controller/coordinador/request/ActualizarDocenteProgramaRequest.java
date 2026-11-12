package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad.request.CorreccionDocumentoRequest;

import java.util.UUID;
import jakarta.validation.constraints.Size;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.NonEmptyUuid;

/**
 * Cuerpo de {@code PUT /api/v1/coordinador/docentes/{idDocente}}. Los campos ausentes no se modifican; el correo no se
 * edita. HU159 (2026-11-05): el tipo y el numero de documento son opcionales y, si cambian, exigen motivo.
 */
public final class ActualizarDocenteProgramaRequest extends CorreccionDocumentoRequest {
    @NonEmptyUuid(message = "El tipo de vinculacion no es valido.")
    private UUID idTipoVinculacion;
    @Size(max = 50, message = "El campo primerNombre excede la longitud permitida.")
    private String primerNombre;
    @Size(max = 50, message = "El campo segundoNombre excede la longitud permitida.")
    private String segundoNombre;
    @Size(max = 50, message = "El campo primerApellido excede la longitud permitida.")
    private String primerApellido;
    @Size(max = 50, message = "El campo segundoApellido excede la longitud permitida.")
    private String segundoApellido;

    public UUID getIdTipoVinculacion() { return idTipoVinculacion; }
    public void setIdTipoVinculacion(final UUID idTipoVinculacion) { this.idTipoVinculacion = idTipoVinculacion; }
    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(final String primerNombre) { this.primerNombre = primerNombre; }
    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(final String segundoNombre) { this.segundoNombre = segundoNombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(final String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(final String segundoApellido) { this.segundoApellido = segundoApellido; }
}
