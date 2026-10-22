package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_candidatos_docente_programa} (HU091, datos minimos). */
@Entity
public class SpDocenteCandidatoEntity {
    @Id @Column(name = "idDocente") private UUID idDocente;
    @Column(name = "idUsuario") private UUID idUsuario;
    @Column(name = "codigoTipoIdentificacion") private String codigoTipoIdentificacion;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;

    protected SpDocenteCandidatoEntity() { }

    public UUID idDocente() { return idDocente; }
    public UUID idUsuario() { return idUsuario; }
    public String codigoTipoIdentificacion() { return codigoTipoIdentificacion; }
    public Integer numeroIdentificacion() { return numeroIdentificacion; }
    public String nombreCompleto() { return nombreCompleto; }
}
