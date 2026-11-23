package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "TipoNotificacion")
public class TipoNotificacionEntity {

    @Id
    @Column(name = "tipo", length = 60)
    private String tipo;
    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;
    @Column(name = "interno", nullable = false)
    private boolean interno;

    protected TipoNotificacionEntity() {
    }

    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public boolean isInterno() { return interno; }
}
