package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Cuerpo de {@code POST /api/v1/coordinador/grupos/previsualizacion-horario} (SES-GEN-01). {@code idGrupo} ausente = grupo nuevo: entonces
 * {@code idAsignatura}, {@code idPeriodoAcademico} e {@code idDocente} son obligatorios. Con {@code idGrupo} se toman del grupo (si se envian y
 * difieren, 400). Nombres canonicos unicos: esta ruta no replica los alias anteriores de crear grupo.
 */
public final class PrevisualizarHorarioGrupoRequest {
    private UUID idGrupo;
    private UUID idAsignatura;
    private UUID idPeriodoAcademico;
    private UUID idDocente;
    @NotEmpty(message = "El horario debe tener al menos una franja.")
    @Size(max = 21, message = "El horario no puede superar 21 franjas por semana.")
    private List<FranjaHorarioRequest> franjas;

    public UUID getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(final UUID idGrupo) {
        this.idGrupo = idGrupo;
    }

    public UUID getIdAsignatura() {
        return idAsignatura;
    }

    public void setIdAsignatura(final UUID idAsignatura) {
        this.idAsignatura = idAsignatura;
    }

    public UUID getIdPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public void setIdPeriodoAcademico(final UUID idPeriodoAcademico) {
        this.idPeriodoAcademico = idPeriodoAcademico;
    }

    public UUID getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(final UUID idDocente) {
        this.idDocente = idDocente;
    }

    public List<FranjaHorarioRequest> getFranjas() {
        return franjas;
    }

    public void setFranjas(final List<FranjaHorarioRequest> franjas) {
        this.franjas = franjas;
    }
}
