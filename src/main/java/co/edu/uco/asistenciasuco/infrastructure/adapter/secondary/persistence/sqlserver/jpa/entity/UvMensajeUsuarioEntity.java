package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Modelo de persistencia de SOLO LECTURA sobre la vista {@code dbo.uv_mensaje_usuario} (LB-008 JPA-06).
 * La vista filtra {@code estaActivo = 1}; la consulta no agrega filtros propios.
 */
@Entity
@Immutable
@Table(name = "uv_mensaje_usuario", schema = "dbo")
public class UvMensajeUsuarioEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "codigo")
    private String codigo;

    @Column(name = "contenido")
    private String contenido;

    @Column(name = "estaActivo")
    private Boolean estaActivo;

    protected UvMensajeUsuarioEntity() {
        // requerido por JPA
    }
}
