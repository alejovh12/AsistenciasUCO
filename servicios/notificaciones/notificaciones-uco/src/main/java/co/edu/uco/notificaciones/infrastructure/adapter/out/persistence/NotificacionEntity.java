package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import co.edu.uco.notificaciones.domain.EstadoEnvio;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Fila de la cola de envio ({@code Notificacion}). Destinatarios y datos viajan como JSON. */
@Entity
@Table(name = "Notificacion")
public class NotificacionEntity {

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "aplicacion", nullable = false, length = 60)
    private String aplicacion;
    @Column(name = "idempotencyKey", nullable = false, length = 120)
    private String idempotencyKey;
    @Column(name = "tipo", nullable = false, length = 60)
    private String tipo;
    @Column(name = "idioma", nullable = false, length = 2)
    private String idioma;
    @Column(name = "asunto", length = 200)
    private String asunto;
    @Column(name = "mensaje", length = 4000)
    private String mensaje;
    @Column(name = "destinatarios", nullable = false, length = Integer.MAX_VALUE)
    private String destinatarios;
    @Column(name = "datos", nullable = false, length = Integer.MAX_VALUE)
    private String datos;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoEnvio estado;
    @Column(name = "intentos", nullable = false)
    private int intentos;
    @Column(name = "creadoEn", nullable = false)
    private Instant creadoEn;
    @Column(name = "proximoIntentoEn")
    private Instant proximoIntentoEn;
    @Column(name = "reservadoHasta")
    private Instant reservadoHasta;
    @Column(name = "reservadoPor", length = 100)
    private String reservadoPor;
    @Column(name = "entregadoEn")
    private Instant entregadoEn;
    @Column(name = "falloEn")
    private Instant falloEn;
    @Column(name = "codigoError", length = 40)
    private String codigoError;
    @Column(name = "detalleError", length = 300)
    private String detalleError;
    @Column(name = "idCorrelacion", length = 64)
    private String idCorrelacion;
    @Column(name = "traceparent", length = 80)
    private String traceparent;

    protected NotificacionEntity() {
    }

    public NotificacionEntity(final UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public String getAplicacion() { return aplicacion; }
    public void setAplicacion(final String aplicacion) { this.aplicacion = aplicacion; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(final String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getTipo() { return tipo; }
    public void setTipo(final String tipo) { this.tipo = tipo; }
    public String getIdioma() { return idioma; }
    public void setIdioma(final String idioma) { this.idioma = idioma; }
    public String getAsunto() { return asunto; }
    public void setAsunto(final String asunto) { this.asunto = asunto; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(final String mensaje) { this.mensaje = mensaje; }
    public String getDestinatarios() { return destinatarios; }
    public void setDestinatarios(final String destinatarios) { this.destinatarios = destinatarios; }
    public String getDatos() { return datos; }
    public void setDatos(final String datos) { this.datos = datos; }
    public EstadoEnvio getEstado() { return estado; }
    public void setEstado(final EstadoEnvio estado) { this.estado = estado; }
    public int getIntentos() { return intentos; }
    public void setIntentos(final int intentos) { this.intentos = intentos; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(final Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getProximoIntentoEn() { return proximoIntentoEn; }
    public void setProximoIntentoEn(final Instant proximoIntentoEn) { this.proximoIntentoEn = proximoIntentoEn; }
    public Instant getReservadoHasta() { return reservadoHasta; }
    public void setReservadoHasta(final Instant reservadoHasta) { this.reservadoHasta = reservadoHasta; }
    public String getReservadoPor() { return reservadoPor; }
    public void setReservadoPor(final String reservadoPor) { this.reservadoPor = reservadoPor; }
    public Instant getEntregadoEn() { return entregadoEn; }
    public void setEntregadoEn(final Instant entregadoEn) { this.entregadoEn = entregadoEn; }
    public Instant getFalloEn() { return falloEn; }
    public void setFalloEn(final Instant falloEn) { this.falloEn = falloEn; }
    public String getCodigoError() { return codigoError; }
    public void setCodigoError(final String codigoError) { this.codigoError = codigoError; }
    public String getDetalleError() { return detalleError; }
    public void setDetalleError(final String detalleError) { this.detalleError = detalleError; }
    public String getIdCorrelacion() { return idCorrelacion; }
    public void setIdCorrelacion(final String idCorrelacion) { this.idCorrelacion = idCorrelacion; }
    public String getTraceparent() { return traceparent; }
    public void setTraceparent(final String traceparent) { this.traceparent = traceparent; }
}
