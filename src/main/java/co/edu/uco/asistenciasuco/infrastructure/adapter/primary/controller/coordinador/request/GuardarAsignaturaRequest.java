package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class GuardarAsignaturaRequest {
    @Size(max = 50, message = "El codigo de la asignatura no puede superar 50 caracteres.")
    private String codigo;
    @Size(max = 50, message = "El nombre de la asignatura no puede superar 50 caracteres.")
    private String nombre;
    @Positive(message = "Los creditos deben ser un entero positivo.")
    private Integer creditos;
    @Positive(message = "El numero de semestre debe ser un entero positivo.")
    private Integer semestreNumero;
    @Size(max = 50, message = "El nombre del area no puede superar 50 caracteres.")
    private String nombreArea;
    @Size(max = 50, message = "El nombre del componente no puede superar 50 caracteres.")
    private String nombreComponente;
    @Size(max = 3, message = "El codigo del caracter no puede superar 3 caracteres.")
    private String codigoCaracter;
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
    public String getCodigoCaracter() { return codigoCaracter; }
    public void setCodigoCaracter(final String codigoCaracter) { this.codigoCaracter = codigoCaracter; }
    public String getNombreComponente() { return nombreComponente; }
    public void setNombreComponente(final String nombreComponente) { this.nombreComponente = nombreComponente; }
}
