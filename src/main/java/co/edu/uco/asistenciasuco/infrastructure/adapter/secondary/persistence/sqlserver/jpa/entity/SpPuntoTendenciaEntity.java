package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpPuntoTendenciaEntity {
    @Id @Column(name = "inicioIntervalo") private LocalDate inicioIntervalo;
    @Column(name = "finIntervalo") private LocalDate finIntervalo;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "porcentajeAusentismo") private BigDecimal porcentajeAusentismo;

    protected SpPuntoTendenciaEntity() {
    }

    public LocalDate inicioIntervalo() {
        return inicioIntervalo;
    }

    public LocalDate finIntervalo() {
        return finIntervalo;
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
