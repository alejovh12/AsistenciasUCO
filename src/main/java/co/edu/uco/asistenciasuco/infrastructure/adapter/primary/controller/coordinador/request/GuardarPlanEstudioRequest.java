package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

public final class GuardarPlanEstudioRequest {
    private String codigo;
    private String nombre;
    public String getCodigo() { return codigo; }
    public void setCodigo(final String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(final String nombre) { this.nombre = nombre; }
}
