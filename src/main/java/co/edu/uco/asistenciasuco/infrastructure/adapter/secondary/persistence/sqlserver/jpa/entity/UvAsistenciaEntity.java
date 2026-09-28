package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_asistencia} (freeze DB).
 */
@Entity
@Immutable
@Table(name = "uv_asistencia", schema = "dbo")
public class UvAsistenciaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idEstudianteGrupo")
    private UUID idEstudianteGrupo;

    @Column(name = "idSesion")
    private UUID idSesion;

    protected UvAsistenciaEntity() {
        // requerido por JPA
    }
}
