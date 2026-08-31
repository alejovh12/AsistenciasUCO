package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;

/**
 * Proyeccion del puerto secundario para filas de dbo.uv_docente.
 */
public final class DocenteAsignacionAcademicaRepositoryProjection {

    private final UUID id;
    private final UUID idUsuario;
    private final Integer numeroIdentificacion;
    private final String nombreCompleto;
    private final boolean estaActivoUsuario;
    private final UUID idInstitucion;
    private final String nombreInstitucion;
    private final UUID idFacultad;
    private final String nombreFacultad;
    private final UUID idPrograma;
    private final String nombrePrograma;
    private final UUID idPlanEstudio;
    private final String inpPlanEstudio;
    private final UUID idAsignatura;
    private final String nombreAsignatura;
    private final UUID idGrupo;
    private final String nombreGrupo;
    private final UUID idPerfil;
    private final String codigoPerfil;
    private final String nombrePerfil;
    private final Integer estaActivoDocente;
    private final String estaActivoTextoDocente;

    public DocenteAsignacionAcademicaRepositoryProjection(
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
            final Integer estaActivoDocente,
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

    public Integer getEstaActivoDocente() {
        return estaActivoDocente;
    }

    public String getEstaActivoTextoDocente() {
        return estaActivoTextoDocente;
    }
}
