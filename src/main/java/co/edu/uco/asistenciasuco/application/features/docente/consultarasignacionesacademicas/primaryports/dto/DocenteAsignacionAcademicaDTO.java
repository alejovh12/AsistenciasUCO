package co.edu.uco.asistenciasuco.application.features.docente.consultarasignacionesacademicas.primaryports.dto;

import java.util.UUID;

/**
 * DTO publico para una asignacion academica consultada de un docente.
 */
public final class DocenteAsignacionAcademicaDTO {

    private UUID id;
    private UUID idUsuario;
    private Integer numeroIdentificacion;
    private String nombreCompleto;
    private boolean estaActivoUsuario;
    private UUID idInstitucion;
    private String nombreInstitucion;
    private UUID idFacultad;
    private String nombreFacultad;
    private UUID idPrograma;
    private String nombrePrograma;
    private UUID idPlanEstudio;
    private String inpPlanEstudio;
    private UUID idAsignatura;
    private String nombreAsignatura;
    private UUID idGrupo;
    private String nombreGrupo;
    private UUID idPerfil;
    private String codigoPerfil;
    private String nombrePerfil;
    private boolean estaActivoDocente;
    private String estaActivoTextoDocente;

    public DocenteAsignacionAcademicaDTO() {
        super();
    }

    public DocenteAsignacionAcademicaDTO(
            final UUID id,
            final UUID idUsuario,
            final Integer numeroIdentificacion,
            final String nombreCompleto,
            final boolean estaActivoUsuario,
            final UUID idInstitucion,
            final String nombreInstitucion,
            final UUID idFacultad,
            final String nombreFacultad,
            final UUID idPrograma,
            final String nombrePrograma,
            final UUID idPlanEstudio,
            final String inpPlanEstudio,
            final UUID idAsignatura,
            final String nombreAsignatura,
            final UUID idGrupo,
            final String nombreGrupo,
            final UUID idPerfil,
            final String codigoPerfil,
            final String nombrePerfil,
            final boolean estaActivoDocente,
            final String estaActivoTextoDocente
    ) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.numeroIdentificacion = numeroIdentificacion;
        this.nombreCompleto = nombreCompleto;
        this.estaActivoUsuario = estaActivoUsuario;
        this.idInstitucion = idInstitucion;
        this.nombreInstitucion = nombreInstitucion;
        this.idFacultad = idFacultad;
        this.nombreFacultad = nombreFacultad;
        this.idPrograma = idPrograma;
        this.nombrePrograma = nombrePrograma;
        this.idPlanEstudio = idPlanEstudio;
        this.inpPlanEstudio = inpPlanEstudio;
        this.idAsignatura = idAsignatura;
        this.nombreAsignatura = nombreAsignatura;
        this.idGrupo = idGrupo;
        this.nombreGrupo = nombreGrupo;
        this.idPerfil = idPerfil;
        this.codigoPerfil = codigoPerfil;
        this.nombrePerfil = nombrePerfil;
        this.estaActivoDocente = estaActivoDocente;
        this.estaActivoTextoDocente = estaActivoTextoDocente;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public boolean isEstaActivoUsuario() {
        return estaActivoUsuario;
    }

    public UUID getIdInstitucion() {
        return idInstitucion;
    }

    public String getNombreInstitucion() {
        return nombreInstitucion;
    }

    public UUID getIdFacultad() {
        return idFacultad;
    }

    public String getNombreFacultad() {
        return nombreFacultad;
    }

    public UUID getIdPrograma() {
        return idPrograma;
    }

    public String getNombrePrograma() {
        return nombrePrograma;
    }

    public UUID getIdPlanEstudio() {
        return idPlanEstudio;
    }

    public String getInpPlanEstudio() {
        return inpPlanEstudio;
    }

    public UUID getIdAsignatura() {
        return idAsignatura;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public UUID getIdGrupo() {
        return idGrupo;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public UUID getIdPerfil() {
        return idPerfil;
    }

    public String getCodigoPerfil() {
        return codigoPerfil;
    }

    public String getNombrePerfil() {
        return nombrePerfil;
    }

    public boolean isEstaActivoDocente() {
        return estaActivoDocente;
    }

    public String getEstaActivoTextoDocente() {
        return estaActivoTextoDocente;
    }
}
