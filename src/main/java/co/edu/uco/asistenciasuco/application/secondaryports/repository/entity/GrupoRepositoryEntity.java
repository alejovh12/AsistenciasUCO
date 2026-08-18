package co.edu.uco.asistenciasuco.application.secondaryports.repository.entity;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Proyeccion de grupo obtenida desde dbo.uv_grupo.
 */
public final class GrupoRepositoryEntity {

    private final UUID id;
    private final String codigo;
    private final String nombre;
    private final UUID idAsignatura;
    private final String nombreAsignatura;
    private final UUID idDocente;
    private final Integer capacidadMaximaPermitida;
    private final Integer estudiantesActivos;
    private final Integer cuposDisponibles;
    private final boolean grupoHabilitado;
    private final LocalDate fechaInicioPeriodoAcademico;
    private final LocalDate fechaFinPeriodoAcademico;

    public GrupoRepositoryEntity(
            final UUID id,
            final String codigo,
            final String nombre,
            final UUID idAsignatura,
            final String nombreAsignatura,
            final UUID idDocente,
            final Integer capacidadMaximaPermitida,
            final Integer estudiantesActivos,
            final Integer cuposDisponibles,
            final boolean grupoHabilitado,
            final LocalDate fechaInicioPeriodoAcademico,
            final LocalDate fechaFinPeriodoAcademico
    ) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.idAsignatura = idAsignatura;
        this.nombreAsignatura = nombreAsignatura;
        this.idDocente = idDocente;
        this.capacidadMaximaPermitida = capacidadMaximaPermitida;
        this.estudiantesActivos = estudiantesActivos;
        this.cuposDisponibles = cuposDisponibles;
        this.grupoHabilitado = grupoHabilitado;
        this.fechaInicioPeriodoAcademico = fechaInicioPeriodoAcademico;
        this.fechaFinPeriodoAcademico = fechaFinPeriodoAcademico;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public UUID getIdAsignatura() {
        return idAsignatura;
    }

    public String getNombreAsignatura() {
        return nombreAsignatura;
    }

    public UUID getIdDocente() {
        return idDocente;
    }

    public Integer getCapacidadMaximaPermitida() {
        return capacidadMaximaPermitida;
    }

    public Integer getEstudiantesActivos() {
        return estudiantesActivos;
    }

    public Integer getCuposDisponibles() {
        return cuposDisponibles;
    }

    public boolean isGrupoHabilitado() {
        return grupoHabilitado;
    }

    public LocalDate getFechaInicioPeriodoAcademico() {
        return fechaInicioPeriodoAcademico;
    }

    public LocalDate getFechaFinPeriodoAcademico() {
        return fechaFinPeriodoAcademico;
    }
}
