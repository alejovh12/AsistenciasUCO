package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_docentes_programa} (HU062/HU089). No es tabla ni vista. */
@Entity
public class SpDocenteProgramaPaginadoEntity {
    @Id @Column(name = "idDocentePrograma") private UUID idDocentePrograma;
    @Column(name = "idDocente") private UUID idDocente;
    @Column(name = "idUsuario") private UUID idUsuario;
    @Column(name = "codigoTipoIdentificacion") private String codigoTipoIdentificacion;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "primerNombre") private String primerNombre;
    @Column(name = "segundoNombre") private String segundoNombre;
    @Column(name = "primerApellido") private String primerApellido;
    @Column(name = "segundoApellido") private String segundoApellido;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "correo") private String correo;
    @Column(name = "idTipoVinculacion") private UUID idTipoVinculacion;
    @Column(name = "codigoTipoVinculacion") private String codigoTipoVinculacion;
    @Column(name = "nombreTipoVinculacion") private String nombreTipoVinculacion;
    @JdbcType(LocalDateTimeJdbcType.class)
    @Column(name = "fechaVinculacionUtc") private LocalDateTime fechaVinculacionUtc;
    @Column(name = "estaActivoDocentePrograma") private Boolean estaActivoDocentePrograma;
    @Column(name = "totalGruposPrograma") private Integer totalGruposPrograma;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpDocenteProgramaPaginadoEntity() { }

    public UUID idDocentePrograma() { return idDocentePrograma; }
    public UUID idDocente() { return idDocente; }
    public UUID idUsuario() { return idUsuario; }
    public String codigoTipoIdentificacion() { return codigoTipoIdentificacion; }
    public Integer numeroIdentificacion() { return numeroIdentificacion; }
    public String primerNombre() { return primerNombre; }
    public String segundoNombre() { return segundoNombre; }
    public String primerApellido() { return primerApellido; }
    public String segundoApellido() { return segundoApellido; }
    public String nombreCompleto() { return nombreCompleto; }
    public String correo() { return correo; }
    public UUID idTipoVinculacion() { return idTipoVinculacion; }
    public String codigoTipoVinculacion() { return codigoTipoVinculacion; }
    public String nombreTipoVinculacion() { return nombreTipoVinculacion; }
    public LocalDateTime fechaVinculacionUtc() { return fechaVinculacionUtc; }
    public Boolean estaActivoDocentePrograma() { return estaActivoDocentePrograma; }
    public Integer totalGruposPrograma() { return totalGruposPrograma; }
    public Integer totalRegistros() { return totalRegistros; }
}
