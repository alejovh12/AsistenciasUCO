package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.request;

/**
 * Cuerpo de {@code PUT /api/v1/decano/coordinadores/{id}} (HU121). No incluye documento ni tipo de documento.
 */
public final class ActualizarCoordinadorRequest {
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
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
