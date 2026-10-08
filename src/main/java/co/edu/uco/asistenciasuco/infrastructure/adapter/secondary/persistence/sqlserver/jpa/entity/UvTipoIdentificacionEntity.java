package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "uv_tipo_identificacion", schema = "dbo")
public class UvTipoIdentificacionEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "tipoIdentificacion") private String tipoIdentificacion;
    @Column(name = "nombre") private String nombre;
    protected UvTipoIdentificacionEntity() { }

    public UUID id() {
        return id;
    }

    public String tipoIdentificacion() {
        return tipoIdentificacion;
    }

    public String nombre() {
        return nombre;
    }
}

