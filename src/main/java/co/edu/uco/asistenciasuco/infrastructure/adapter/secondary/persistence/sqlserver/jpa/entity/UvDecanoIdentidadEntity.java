package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_decano_identidad} (LB-008 JPA-05).
 * Solo se mapean las columnas que consume la autorizacion.
 */
@Entity
@Immutable
@Table(name = "uv_decano_identidad", schema = "dbo")
public class UvDecanoIdentidadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idUsuario")
    private UUID idUsuario;

    protected UvDecanoIdentidadEntity() {
        // requerido por JPA
    }
}
