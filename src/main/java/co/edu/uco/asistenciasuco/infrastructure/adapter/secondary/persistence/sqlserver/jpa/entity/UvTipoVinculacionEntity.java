package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_tipo_vinculacion", schema = "dbo")
public class UvTipoVinculacionEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "codigo") private String codigo;
    @Column(name = "nombre") private String nombre;
    @Column(name = "estaActivoTipoVinculacion") private Boolean estaActivoTipoVinculacion;

    protected UvTipoVinculacionEntity() { }

    public UUID id() { return id; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public Boolean estaActivoTipoVinculacion() { return estaActivoTipoVinculacion; }
}
