package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpAlertaTempranaEntity {
    @Id @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "correo") private String correo;
    @Column(name = "totalMatriculasActivas") private Integer totalMatriculasActivas;
    @Column(name = "porcentajeInasistencia") private BigDecimal porcentajeInasistencia;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "codigoPermanencia") private String codigoPermanencia;
    @Column(name = "semaforo") private String semaforo;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpAlertaTempranaEntity() {
    }

    public UUID idEstudiante() {
        return idEstudiante;
    }

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String correo() {
        return correo;
    }

    public Integer totalMatriculasActivas() {
        return totalMatriculasActivas;
    }

    public BigDecimal porcentajeInasistencia() {
        return porcentajeInasistencia;
    }

    public Integer faltasSinJustaCausa() {
        return faltasSinJustaCausa;
    }

    public String codigoPermanencia() {
        return codigoPermanencia;
    }

    public String semaforo() {
        return semaforo;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
