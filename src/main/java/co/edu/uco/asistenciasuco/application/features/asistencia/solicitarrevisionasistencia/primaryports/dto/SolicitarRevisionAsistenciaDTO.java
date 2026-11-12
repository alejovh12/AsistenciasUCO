package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para solicitar la revision de una asistencia.
 */
public final class SolicitarRevisionAsistenciaDTO {

    private UUID sesion;
    private String categoria;
    private String justificacion;
    private List<UUID> soportes;
    private UUID usuario;
    /** PC (puntos de control): toma reclamada; nulo es el reclamo de la sesion (legado). */
    private UUID toma;

    public SolicitarRevisionAsistenciaDTO() {
        super();
    }

    public SolicitarRevisionAsistenciaDTO(
            final UUID sesion,
            final String categoria,
            final String justificacion,
            final List<UUID> soportes,
            final UUID usuario
    ) {
        setSesion(sesion);
        setCategoria(categoria);
        setJustificacion(justificacion);
        setSoportes(soportes);
        setUsuario(usuario);
    }

    public SolicitarRevisionAsistenciaDTO(
            final UUID sesion,
            final UUID toma,
            final String categoria,
            final String justificacion,
            final List<UUID> soportes,
            final UUID usuario
    ) {
        this(sesion, categoria, justificacion, soportes, usuario);
        setToma(toma);
    }

    public UUID getToma() {
        return toma;
    }

    public void setToma(final UUID toma) {
        this.toma = toma;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(final String categoria) {
        this.categoria = categoria;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(final String justificacion) {
        this.justificacion = justificacion;
    }

    /**
     * CULM-001 D.2 (CONTRACT D 3.3): identificadores opacos (fileId) de 0 a N soportes ya subidos, en el orden recibido. Nunca una
     * URL ni un nombre de archivo. Nulo equivale a ningun soporte.
     */
    public List<UUID> getSoportes() {
        return soportes;
    }

    public void setSoportes(final List<UUID> soportes) {
        this.soportes = soportes;
    }

    public UUID getUsuario() {
        return usuario;
    }

    public void setUsuario(final UUID usuario) {
        this.usuario = usuario;
    }

}
