package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO publico de grupo consultado.
 */
public final class GrupoDTO {

    private UUID id;
    private String codigo;
    private String nombre;
    private UUID idAsignatura;
    private String nombreAsignatura;
    private UUID idDocente;
    private Integer capacidadMaximaPermitida;
    private Integer estudiantesActivos;
    private Integer cuposDisponibles;
    private boolean grupoHabilitado;
    private LocalDate fechaInicioPeriodoAcademico;
    private LocalDate fechaFinPeriodoAcademico;

    public GrupoDTO() {
        super();
    }

    public GrupoDTO(
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
