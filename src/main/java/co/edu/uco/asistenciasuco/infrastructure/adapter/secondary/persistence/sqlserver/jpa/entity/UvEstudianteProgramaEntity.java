package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Mapeo de {@code dbo.uv_estudiante_programa} (LB-008 JPA-05).
 * Identidad: {@code EstudiantePrograma.id}. La vista garantiza una fila por EstudiantePrograma (DB JPA-05).
 */
@Entity
@Immutable
@Table(name = "uv_estudiante_programa", schema = "dbo")
public class UvEstudianteProgramaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "idEstudiante", nullable = false)
    private UUID idEstudiante;

    @Column(name = "nombreEstudiante", nullable = false)
    private String nombreEstudiante;

    @Column(name = "idPrograma", nullable = false)
    private UUID idPrograma;

    @Column(name = "nombrePrograma", nullable = false)
    private String nombrePrograma;

    protected UvEstudianteProgramaEntity() {
    }
}
