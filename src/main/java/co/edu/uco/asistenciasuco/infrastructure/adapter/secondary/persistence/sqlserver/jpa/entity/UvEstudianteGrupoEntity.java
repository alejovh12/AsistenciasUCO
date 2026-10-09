package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_estudiante_grupo} (freeze DB).
 */
@Entity
@Immutable
@Table(name = "uv_estudiante_grupo", schema = "dbo")
public class UvEstudianteGrupoEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idEstudiante")
    private UUID idEstudiante;

    @Column(name = "nombreCompletoEstudiante")
    private String nombreCompletoEstudiante;

    @Column(name = "idGrupo")
    private UUID idGrupo;

    @Column(name = "codigoEstadoEstudiante")
    private String codigoEstadoEstudiante;

    @Column(name = "nombreEstadoEstudiante")
    private String nombreEstadoEstudiante;

    protected UvEstudianteGrupoEntity() {
        // requerido por JPA
    }
}
