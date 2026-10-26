package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

/** Fila de {@code dbo.usp_consultar_estudiantes_facultad_paginado} (HU104/HU105). */
@Entity
public class SpEstudianteFacultadPaginadoEntity {
    @Id @Column(name = "idEstudiante") private UUID idEstudiante;
    @Column(name = "numeroIdentificacion") private Integer numeroIdentificacion;
    @Column(name = "nombreCompleto") private String nombreCompleto;
    @Column(name = "correo") private String correo;
    @Column(name = "estaActivoUsuario") private Boolean estaActivoUsuario;
    @Column(name = "programas") private String programas;
    @Column(name = "totalGrupos") private Integer totalGrupos;
    @Column(name = "totalRegistros") private Integer totalRegistros;

    protected SpEstudianteFacultadPaginadoEntity() {
    }

    public UUID idEstudiante() {
        return idEstudiante;
    }

    public Integer numeroIdentificacion() {
        return numeroIdentificacion;
    }

    public String nombreCompleto() {
        return nombreCompleto;
    }

    public String correo() {
        return correo;
    }

    public Boolean estaActivoUsuario() {
        return estaActivoUsuario;
    }

    public String programas() {
        return programas;
    }

    public Integer totalGrupos() {
        return totalGrupos;
    }

    public Integer totalRegistros() {
        return totalRegistros;
    }
}
