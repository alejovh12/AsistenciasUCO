package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import co.edu.uco.notificaciones.domain.EstadoEnvio;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, UUID> {

    Optional<NotificacionEntity> findByAplicacionAndIdempotencyKey(String aplicacion, String idempotencyKey);

    /** Elegibles para despacho con bloqueo que salta filas ya tomadas por otra instancia (UPDLOCK + READPAST). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select n from NotificacionEntity n where (n.estado in :reintentables and n.proximoIntentoEn <= :ahora) "
            + "or (n.estado = :reservada and n.reservadoHasta < :ahora) order by n.creadoEn asc")
    List<NotificacionEntity> elegibles(@Param("reintentables") Collection<EstadoEnvio> reintentables,
                                       @Param("reservada") EstadoEnvio reservada,
                                       @Param("ahora") Instant ahora,
                                       Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update NotificacionEntity n set n.estado = :entregada, n.entregadoEn = :ahora, n.reservadoHasta = null, "
            + "n.reservadoPor = null, n.codigoError = null, n.detalleError = null, n.proximoIntentoEn = null "
            + "where n.id = :id and n.estado = :reservada and n.reservadoPor = :instancia")
    int marcarEntregada(@Param("id") UUID id, @Param("instancia") String instancia, @Param("ahora") Instant ahora,
                        @Param("entregada") EstadoEnvio entregada, @Param("reservada") EstadoEnvio reservada);

    @Query("select n.id from NotificacionEntity n where n.estado in :terminales and n.creadoEn < :limite order by n.creadoEn asc")
    List<UUID> terminalesAnteriores(@Param("terminales") Collection<EstadoEnvio> terminales, @Param("limite") Instant limite,
                                    Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificacionEntity n where n.id in :ids")
    int eliminarPorIds(@Param("ids") Collection<UUID> ids);

    @Query("select max(n.entregadoEn) from NotificacionEntity n")
    Optional<Instant> ultimaEntrega();

    @Query("select n from NotificacionEntity n where n.falloEn is not null order by n.falloEn desc")
    List<NotificacionEntity> ultimosFallos(Pageable pageable);
}
