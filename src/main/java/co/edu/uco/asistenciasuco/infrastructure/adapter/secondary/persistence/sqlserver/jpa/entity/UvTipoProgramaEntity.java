package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "uv_tipo_programa", schema = "dbo")
public class UvTipoProgramaEntity {
    @Id @Column(name = "id") private UUID id;
    @Column(name = "nombre") private String nombre;
    @Column(name = "codigo") private String codigo;
    @Column(name = "estaActivoTipoPrograma") private Boolean estaActivoTipoPrograma;

    protected UvTipoProgramaEntity() {
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String codigo() {
        return codigo;
    }

    public Boolean estaActivoTipoPrograma() {
        return estaActivoTipoPrograma;
    }
}
