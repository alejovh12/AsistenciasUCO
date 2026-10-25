package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpComparativaFacultadEntity {
    @Id @Column(name = "idFacultad") private UUID idFacultad;
    @Column(name = "nombreFacultad") private String nombreFacultad;
    @Column(name = "estaActivaFacultad") private Boolean estaActivaFacultad;
    @Column(name = "totalProgramas") private Integer totalProgramas;
    @Column(name = "totalEstudiantes") private Integer totalEstudiantes;
    @Column(name = "estudiantesRiesgo") private Integer estudiantesRiesgo;
    @Column(name = "estudiantesPerdida") private Integer estudiantesPerdida;
    @Column(name = "tasaRetencion") private BigDecimal tasaRetencion;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "porcentajeAusentismo") private BigDecimal porcentajeAusentismo;

    protected SpComparativaFacultadEntity() {
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public Boolean estaActivaFacultad() {
        return estaActivaFacultad;
    }

    public Integer totalProgramas() {
        return totalProgramas;
    }

    public Integer totalEstudiantes() {
        return totalEstudiantes;
    }

    public Integer estudiantesRiesgo() {
        return estudiantesRiesgo;
    }

    public Integer estudiantesPerdida() {
        return estudiantesPerdida;
    }

    public BigDecimal tasaRetencion() {
        return tasaRetencion;
    }

    public Integer sesionesEjecutadas() {
        return sesionesEjecutadas;
    }

    public Integer faltasSinJustaCausa() {
        return faltasSinJustaCausa;
    }

    public BigDecimal porcentajeAusentismo() {
        return porcentajeAusentismo;
    }
}
