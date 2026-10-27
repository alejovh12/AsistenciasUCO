package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpComparativaProgramaEntity {
    @Id @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "estaActivoPrograma") private Boolean estaActivoPrograma;
    @Column(name = "totalEstudiantes") private Integer totalEstudiantes;
    @Column(name = "estudiantesRiesgo") private Integer estudiantesRiesgo;
    @Column(name = "estudiantesPerdida") private Integer estudiantesPerdida;
    @Column(name = "tasaRetencion") private BigDecimal tasaRetencion;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "porcentajeAusentismo") private BigDecimal porcentajeAusentismo;

    protected SpComparativaProgramaEntity() {
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public Boolean estaActivoPrograma() {
        return estaActivoPrograma;
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
