package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "PlantillaNotificacion")
public class PlantillaEntity {

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;
    @Column(name = "idioma", nullable = false, length = 2)
    private String idioma;
    @Column(name = "version", nullable = false)
    private int version;
    @Column(name = "asunto", nullable = false, length = 200)
    private String asunto;
    @Column(name = "cuerpo", nullable = false, length = 4000)
    private String cuerpo;
    @Column(name = "activa", nullable = false)
    private boolean activa;
    @Column(name = "creadoPor", length = 100)
    private String creadoPor;
    @Column(name = "creadoEn", nullable = false)
    private Instant creadoEn;

    protected PlantillaEntity() {
    }

    public PlantillaEntity(final UUID id, final String tipo, final String idioma, final int version, final String asunto,
                           final String cuerpo, final boolean activa, final String creadoPor, final Instant creadoEn) {
        this.id = id;
        this.tipo = tipo;
        this.idioma = idioma;
        this.version = version;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.activa = activa;
        this.creadoPor = creadoPor;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public String getIdioma() { return idioma; }
    public int getVersion() { return version; }
    public String getAsunto() { return asunto; }
    public String getCuerpo() { return cuerpo; }
    public boolean isActiva() { return activa; }
    public void setActiva(final boolean activa) { this.activa = activa; }
    public String getCreadoPor() { return creadoPor; }
    public Instant getCreadoEn() { return creadoEn; }
}
