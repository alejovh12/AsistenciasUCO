package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_parametro} (LB-008 JPA-05).
 * La vista filtra {@code estaActivo = 1}; la consulta no agrega filtros propios.
 */
@Entity
@Immutable
@Table(name = "uv_parametro", schema = "dbo")
public class UvParametroEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "grupo")
    private String grupo;

    @Column(name = "clave")
    private String clave;

    @Column(name = "valor")
    private String valor;

    @Column(name = "tipoDato")
    private String tipoDato;

    @Column(name = "valorDefecto")
    private String valorDefecto;

    @Column(name = "estaActivo")
    private Boolean estaActivo;

    protected UvParametroEntity() {
        // requerido por JPA
    }

    public UUID id() {
        return id;
    }

    public String grupo() {
        return grupo;
    }

    public String clave() {
        return clave;
    }

    public String valor() {
        return valor;
    }

    public String tipoDato() {
        return tipoDato;
    }

    public String valorDefecto() {
        return valorDefecto;
    }

    public Boolean estaActivo() {
        return estaActivo;
    }
}

