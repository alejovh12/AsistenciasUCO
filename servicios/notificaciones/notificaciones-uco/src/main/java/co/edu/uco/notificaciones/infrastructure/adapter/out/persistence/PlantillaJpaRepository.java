package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantillaJpaRepository extends JpaRepository<PlantillaEntity, UUID> {

    List<PlantillaEntity> findByTipoAndIdiomaOrderByVersionDesc(String tipo, String idioma);

    List<PlantillaEntity> findByIdiomaOrderByTipoAscVersionDesc(String idioma);

    Optional<PlantillaEntity> findByTipoAndIdiomaAndActivaTrue(String tipo, String idioma);

    Optional<PlantillaEntity> findByTipoAndIdiomaAndVersion(String tipo, String idioma, int version);

    @Query("select coalesce(max(p.version), 0) from PlantillaEntity p where p.tipo = :tipo and p.idioma = :idioma")
    int maxVersion(@Param("tipo") String tipo, @Param("idioma") String idioma);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlantillaEntity p set p.activa = false where p.tipo = :tipo and p.idioma = :idioma and p.activa = true and p.id <> :excepto")
    int desactivarOtras(@Param("tipo") String tipo, @Param("idioma") String idioma, @Param("excepto") UUID excepto);
}
