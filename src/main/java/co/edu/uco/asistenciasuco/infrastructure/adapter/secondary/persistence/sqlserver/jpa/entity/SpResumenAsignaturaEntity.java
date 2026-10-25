package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpResumenAsignaturaEntity {
    @Id @Column(name = "idGrupo") private UUID idGrupo;
    @Column(name = "codigoGrupo") private Integer codigoGrupo;
    @Column(name = "idAsignatura") private UUID idAsignatura;
    @Column(name = "nombreAsignatura") private String nombreAsignatura;
    @Column(name = "codigoEstadoEstudiante") private String codigoEstadoEstudiante;
    @Column(name = "nombreEstadoEstudiante") private String nombreEstadoEstudiante;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "asistencias") private Integer asistencias;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "excusas") private Integer excusas;
    @Column(name = "porcentajeInasistencia") private BigDecimal porcentajeInasistencia;
    @Column(name = "codigoPermanencia") private String codigoPermanencia;

    protected SpResumenAsignaturaEntity() {
    }

    public UUID idGrupo() {
        return idGrupo;
    }

    public Integer codigoGrupo() {
        return codigoGrupo;
    }

    public UUID idAsignatura() {
        return idAsignatura;
    }

    public String nombreAsignatura() {
        return nombreAsignatura;
    }

    public String codigoEstadoEstudiante() {
        return codigoEstadoEstudiante;
    }

    public String nombreEstadoEstudiante() {
        return nombreEstadoEstudiante;
    }

    public Integer sesionesEjecutadas() {
        return sesionesEjecutadas;
    }

    public Integer asistencias() {
        return asistencias;
    }

    public Integer faltasSinJustaCausa() {
        return faltasSinJustaCausa;
    }

    public Integer excusas() {
        return excusas;
    }

    public BigDecimal porcentajeInasistencia() {
        return porcentajeInasistencia;
    }

    public String codigoPermanencia() {
        return codigoPermanencia;
    }
}
