package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_identidad_externa} (SEC-BASELINE-017).
 * Solo se mapean las columnas que consume la resolución de identidad; nunca tokens ni secretos.
 */
@Entity
@Immutable
@Table(name = "uv_identidad_externa", schema = "dbo")
public class UvIdentidadExternaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "idUsuario")
    private UUID idUsuario;

    @Column(name = "emisor")
    private String emisor;

    @Column(name = "sujeto")
    private String sujeto;

    @Column(name = "estaVigenteIdentidad")
    private Boolean estaVigenteIdentidad;

    protected UvIdentidadExternaEntity() {
        // requerido por JPA
    }
}
