package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarcadorJpaRepository extends JpaRepository<MarcadorEntity, UUID> {

    List<MarcadorEntity> findByTipoOrderByObligatorioDescMarcadorAsc(String tipo);

    List<MarcadorEntity> findAllByOrderByTipoAscObligatorioDescMarcadorAsc();
}
