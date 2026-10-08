package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_asignatura} (LB-008 JPA-05).
 */
@Entity
@Immutable
@Table(name = "uv_asignatura", schema = "dbo")
public class UvAsignaturaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "credito")
    private Integer credito;

    @Column(name = "idArea")
    private UUID idArea;

    @Column(name = "nombreArea")
    private String nombreArea;

    @Column(name = "idComponente")
    private UUID idComponente;

    @Column(name = "nombreComponente")
    private String nombreComponente;

    @Column(name = "idSemestrePlanEstudio")
    private UUID idSemestrePlanEstudio;

    @Column(name = "nombrePrograma")
    private String nombrePrograma;

    @Column(name = "codigoSemestre")
    private String codigoSemestre;

    @Column(name = "estaActivaAsignatura")
    private Boolean estaActivaAsignatura;

    @Column(name = "estaActivaTextoAsignatura")
    private String estaActivaTextoAsignatura;

    protected UvAsignaturaEntity() {
        // requerido por JPA
    }
}
