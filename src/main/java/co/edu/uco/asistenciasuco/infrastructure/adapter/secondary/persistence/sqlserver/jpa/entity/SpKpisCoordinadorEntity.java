package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpKpisCoordinadorEntity {
    @Id @Column(name = "idPrograma") private UUID idPrograma;
    @Column(name = "nombrePrograma") private String nombrePrograma;
    @Column(name = "idPeriodoAcademico") private UUID idPeriodoAcademico;
    @Column(name = "nombrePeriodoAcademico") private String nombrePeriodoAcademico;
    @Column(name = "totalEstudiantes") private Integer totalEstudiantes;
    @Column(name = "estudiantesRegulares") private Integer estudiantesRegulares;
    @Column(name = "estudiantesRiesgo") private Integer estudiantesRiesgo;
    @Column(name = "estudiantesPerdida") private Integer estudiantesPerdida;
    @Column(name = "tasaPermanencia") private BigDecimal tasaPermanencia;
    @Column(name = "sesionesProgramadas") private Integer sesionesProgramadas;
    @Column(name = "sesionesVencidas") private Integer sesionesVencidas;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "sesionesVencidasSinRegistro") private Integer sesionesVencidasSinRegistro;
    @Column(name = "sesionesSinProcedenciaTemporal") private Integer sesionesSinProcedenciaTemporal;
    @Column(name = "porcentajeCumplimiento") private BigDecimal porcentajeCumplimiento;
    @Column(name = "porcentajeProgresoCalendario") private BigDecimal porcentajeProgresoCalendario;
    @Column(name = "umbralAdvertencia") private BigDecimal umbralAdvertencia;
    @Column(name = "umbralPerdida") private BigDecimal umbralPerdida;

    // CIERRE-W3I: columnas nuevas al final del SP (se leen por nombre).
    @Column(name = "totalDocentes") private Integer totalDocentes;
    @Column(name = "gruposActivos") private Integer gruposActivos;
    @Column(name = "cupoTotal") private Integer cupoTotal;
    @Column(name = "matriculadosActivos") private Integer matriculadosActivos;
    @Column(name = "tasaAforo") private BigDecimal tasaAforo;
    @Column(name = "estudiantesRegistrados") private Integer estudiantesRegistrados;

    protected SpKpisCoordinadorEntity() {
    }

    public UUID idPrograma() {
        return idPrograma;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public String nombrePeriodoAcademico() {
        return nombrePeriodoAcademico;
    }

    public Integer totalEstudiantes() {
        return totalEstudiantes;
    }

    public Integer estudiantesRegulares() {
        return estudiantesRegulares;
    }

    public Integer estudiantesRiesgo() {
        return estudiantesRiesgo;
    }

    public Integer estudiantesPerdida() {
        return estudiantesPerdida;
    }

    public BigDecimal tasaPermanencia() {
        return tasaPermanencia;
    }

    public Integer sesionesProgramadas() {
        return sesionesProgramadas;
    }

    public Integer sesionesVencidas() {
        return sesionesVencidas;
    }

    public Integer sesionesEjecutadas() {
        return sesionesEjecutadas;
    }

    public Integer sesionesVencidasSinRegistro() {
        return sesionesVencidasSinRegistro;
    }

    public Integer sesionesSinProcedenciaTemporal() {
        return sesionesSinProcedenciaTemporal;
    }

    public BigDecimal porcentajeCumplimiento() {
        return porcentajeCumplimiento;
    }

    public BigDecimal porcentajeProgresoCalendario() {
        return porcentajeProgresoCalendario;
    }

    public BigDecimal umbralAdvertencia() {
        return umbralAdvertencia;
    }

    public BigDecimal umbralPerdida() {
        return umbralPerdida;
    }

    public Integer totalDocentes() {
        return totalDocentes;
    }

    public Integer gruposActivos() {
        return gruposActivos;
    }

    public Integer cupoTotal() {
        return cupoTotal;
    }

    public Integer matriculadosActivos() {
        return matriculadosActivos;
    }

    public BigDecimal tasaAforo() {
        return tasaAforo;
    }

    public Integer estudiantesRegistrados() {
        return estudiantesRegistrados;
    }
}
