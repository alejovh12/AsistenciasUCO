package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request;

import java.util.UUID;

public final class ConsultarEstudiantesRequest {

    private final UUID tipoIdentificacionId;
    private final Integer numeroIdentificacion;
    private final String nombre;
    private final String correo;
    private final UUID institucionId;
    private final UUID facultadId;
    private final UUID programaId;
    private final UUID grupoId;
    private final Boolean activo;
    private final Integer page;
    private final Integer size;

    public ConsultarEstudiantesRequest(
            final UUID tipoIdentificacionId,
            final Integer numeroIdentificacion,
            final String nombre,
            final String correo,
            final UUID institucionId,
            final UUID facultadId,
            final UUID programaId,
            final UUID grupoId,
            final Boolean activo,
            final Integer page,
            final Integer size
    ) {
        this.tipoIdentificacionId = tipoIdentificacionId;
        this.numeroIdentificacion = numeroIdentificacion;
        this.nombre = nombre;
        this.correo = correo;
        this.institucionId = institucionId;
        this.facultadId = facultadId;
        this.programaId = programaId;
        this.grupoId = grupoId;
        this.activo = activo;
        this.page = page;
        this.size = size;
    }

    public UUID getTipoIdentificacionId() {
        return tipoIdentificacionId;
    }

    public Integer getNumeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public UUID getInstitucionId() {
        return institucionId;
    }

    public UUID getFacultadId() {
        return facultadId;
    }

    public UUID getProgramaId() {
        return programaId;
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    public Boolean getActivo() {
        return activo;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }
}
