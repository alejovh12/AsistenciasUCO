package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_facultad} (LB-008 JPA-05).
 * {@code idDecano} y {@code nombreCompletoDecano} son NULLABLE en la vista.
 */
@Entity
@Immutable
@Table(name = "uv_facultad", schema = "dbo")
public class UvFacultadEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "nombreFacultad")
    private String nombreFacultad;

    @Column(name = "idInstitucion")
    private UUID idInstitucion;

    @Column(name = "nombreInstitucion")
    private String nombreInstitucion;

    @Column(name = "idDecano")
    private UUID idDecano;

    @Column(name = "nombreCompletoDecano")
    private String nombreCompletoDecano;

    @Column(name = "estaActivaFacultad")
    private Boolean estaActivaFacultad;

    @Column(name = "estaActivaTextoFacultad")
    private String estaActivaTextoFacultad;

    protected UvFacultadEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public UUID idInstitucion() {
        return idInstitucion;
    }

    public String nombreInstitucion() {
        return nombreInstitucion;
    }

    public UUID idDecano() {
        return idDecano;
    }

    public String nombreCompletoDecano() {
        return nombreCompletoDecano;
    }

    public Boolean estaActivaFacultad() {
        return estaActivaFacultad;
    }

    public String estaActivaTextoFacultad() {
        return estaActivaTextoFacultad;
    }
}

