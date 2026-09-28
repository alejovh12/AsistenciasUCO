package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request;

import java.util.List;
import java.util.UUID;

public final class ActualizarGrupoRequest {

    private Integer codigo;
    private String nombre;
    private UUID idDocente;
    private UUID docenteId;
    private Integer cupoMaximo;
    private List<String> dias;
    private String horaInicio;
    private String horaFin;

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(final Integer codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public void setSection(final String section) {
        this.nombre = section;
    }

    public UUID getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(final UUID idDocente) {
        this.idDocente = idDocente;
    }

    public UUID getDocenteId() {
        return docenteId;
    }

    public void setDocenteId(final UUID docenteId) {
        this.docenteId = docenteId;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(final Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public List<String> getDias() {
        return dias;
    }

    public void setDias(final List<String> dias) {
        this.dias = dias;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(final String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(final String horaFin) {
        this.horaFin = horaFin;
    }
}
