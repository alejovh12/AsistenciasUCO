package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_programa}. Solo expone lo necesario para
 * resolver el ambito de facultad (que programas pertenecen a la facultad de un decano).
 */
@Entity
@Immutable
@Table(name = "uv_programa", schema = "dbo")
public class UvProgramaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "idFacultad")
    private UUID idFacultad;

    protected UvProgramaEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String nombrePrograma() {
        return nombrePrograma;
    }

    public UUID idFacultad() {
        return idFacultad;
    }
}
