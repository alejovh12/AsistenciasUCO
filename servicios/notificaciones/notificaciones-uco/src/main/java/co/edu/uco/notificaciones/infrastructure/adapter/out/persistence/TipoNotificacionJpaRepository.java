package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoNotificacionJpaRepository extends JpaRepository<TipoNotificacionEntity, String> {

    List<TipoNotificacionEntity> findByInternoFalseOrderByTipoAsc();
}
