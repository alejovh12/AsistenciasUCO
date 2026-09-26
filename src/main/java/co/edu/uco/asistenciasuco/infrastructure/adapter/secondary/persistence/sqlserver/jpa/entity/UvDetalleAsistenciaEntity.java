package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_detalle_asistencia}
 * (freeze DB). No es un objeto de dominio, no tiene relaciones y no expone API de escritura.
 * Solo se mapean las columnas que necesita la query piloto de asistencia.
 */
@Entity
@Immutable
@Table(name = "uv_detalle_asistencia", schema = "dbo")
public class UvDetalleAsistenciaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idAsistencia")
    private UUID idAsistencia;

    /** Nullable en el mapeo (defensivo): la semantica publica sigue siendo {@code ResultSet#getBoolean}. */
    @Column(name = "asistio")
    private Boolean asistio;

    @Column(name = "codigoRazonCausa")
    private String codigoRazonCausa;

    protected UvDetalleAsistenciaEntity() {
        // requerido por JPA
    }
}
