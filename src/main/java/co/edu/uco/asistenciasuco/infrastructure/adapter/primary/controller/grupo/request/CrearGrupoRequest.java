package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request;

import java.util.List;
import java.util.UUID;

public final class CrearGrupoRequest {

    private UUID idAsignatura;
    private UUID asignaturaId;
    private UUID idPeriodoAcademico;
    private UUID periodoAcademicoId;
    private Integer codigo;
    private String nombre;
    private UUID idDocente;
    private UUID docenteId;
    private String aula;
    private List<String> dias;
    private String horaInicio;
    private String horaFin;
    private Boolean generarSesionesAutomaticas;
    private Boolean crearSesionesAutomaticamente;

    public UUID getIdAsignatura() {
        return idAsignatura;
    }

    public void setIdAsignatura(final UUID idAsignatura) {
        this.idAsignatura = idAsignatura;
    }

    public UUID getAsignaturaId() {
        return asignaturaId;
    }

    public void setAsignaturaId(final UUID asignaturaId) {
        this.asignaturaId = asignaturaId;
    }

    public UUID getIdPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public void setIdPeriodoAcademico(final UUID idPeriodoAcademico) {
        this.idPeriodoAcademico = idPeriodoAcademico;
    }

    public UUID getPeriodoAcademicoId() {
        return periodoAcademicoId;
    }

    public void setPeriodoAcademicoId(final UUID periodoAcademicoId) {
        this.periodoAcademicoId = periodoAcademicoId;
    }

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

    public String getAula() {
        return aula;
    }

    public void setAula(final String aula) {
        this.aula = aula;
    }

    public void setRoom(final String room) {
        this.aula = room;
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

    public Boolean getGenerarSesionesAutomaticas() {
        return generarSesionesAutomaticas;
    }

    public void setGenerarSesionesAutomaticas(final Boolean generarSesionesAutomaticas) {
        this.generarSesionesAutomaticas = generarSesionesAutomaticas;
    }

    public Boolean getCrearSesionesAutomaticamente() {
        return crearSesionesAutomaticamente;
    }

    public void setCrearSesionesAutomaticamente(final Boolean crearSesionesAutomaticamente) {
        this.crearSesionesAutomaticamente = crearSesionesAutomaticamente;
    }
}
