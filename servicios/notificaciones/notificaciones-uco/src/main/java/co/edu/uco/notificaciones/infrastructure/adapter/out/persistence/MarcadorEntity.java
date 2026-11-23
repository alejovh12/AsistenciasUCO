package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "TipoNotificacionMarcador")
public class MarcadorEntity {

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;
    @Column(name = "marcador", nullable = false, length = 40)
    private String marcador;
    @Column(name = "descripcion", nullable = false, length = 200)
    private String descripcion;
    @Column(name = "ejemplo", nullable = false, length = 300)
    private String ejemplo;
    @Column(name = "obligatorio", nullable = false)
    private boolean obligatorio;

    protected MarcadorEntity() {
    }

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public String getMarcador() { return marcador; }
    public String getDescripcion() { return descripcion; }
    public String getEjemplo() { return ejemplo; }
    public boolean isObligatorio() { return obligatorio; }
}
