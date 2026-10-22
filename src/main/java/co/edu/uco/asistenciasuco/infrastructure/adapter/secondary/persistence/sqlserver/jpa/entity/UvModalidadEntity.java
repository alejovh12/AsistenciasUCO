package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "uv_modalidad", schema = "dbo")
public class UvModalidadEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "codigo") private String codigo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "estaActivoModalidad") private Boolean estaActivoModalidad;

    protected UvModalidadEntity() {
    }

    public UUID id() {
        return id;
    }

    public String codigo() {
        return codigo;
    }

    public String nombre() {
        return nombre;
    }

    public Boolean estaActivoModalidad() {
        return estaActivoModalidad;
    }
}
