package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

public final class GuardarAsignaturaRequest {
    private String codigo;
    private String nombre;
    private Integer creditos;
    private Integer semestreNumero;
    private String nombreArea;
    private String nombreComponente;
    public String getCodigo() { return codigo; }
    public void setCodigo(final String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(final String nombre) { this.nombre = nombre; }
    public Integer getCreditos() { return creditos; }
    public void setCreditos(final Integer creditos) { this.creditos = creditos; }
    public Integer getSemestreNumero() { return semestreNumero; }
    public void setSemestreNumero(final Integer semestreNumero) { this.semestreNumero = semestreNumero; }
    public String getNombreArea() { return nombreArea; }
    public void setNombreArea(final String nombreArea) { this.nombreArea = nombreArea; }
    public String getNombreComponente() { return nombreComponente; }
    public void setNombreComponente(final String nombreComponente) { this.nombreComponente = nombreComponente; }
}
