package co.edu.uco.notificaciones.infrastructure.adapter.out.persistence;

import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.TipoNotificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA de tipos, marcadores y versiones de plantillas. */
@Repository
@Transactional(readOnly = true)
public class PlantillaRepositoryAdapter implements PlantillaRepositoryPort {

    private final TipoNotificacionJpaRepository tipos;
    private final MarcadorJpaRepository marcadores;
    private final PlantillaJpaRepository plantillas;

    @PersistenceContext
    private EntityManager entityManager;

    public PlantillaRepositoryAdapter(final TipoNotificacionJpaRepository tipos, final MarcadorJpaRepository marcadores,
                                      final PlantillaJpaRepository plantillas) {
        this.tipos = tipos;
        this.marcadores = marcadores;
        this.plantillas = plantillas;
    }

    @Override
    public List<TipoNotificacion> listarTipos() {
        return tipos.findByInternoFalseOrderByTipoAsc().stream().map(PlantillaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Optional<TipoNotificacion> buscarTipo(final String tipo) {
        return tipo == null || tipo.isBlank() ? Optional.empty() : tipos.findById(tipo).map(PlantillaRepositoryAdapter::aDominio);
    }

    @Override
    public List<Marcador> marcadores(final String tipo) {
        return marcadores.findByTipoOrderByObligatorioDescMarcadorAsc(tipo).stream().map(PlantillaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Map<String, List<Marcador>> marcadoresPorTipo() {
        final Map<String, List<Marcador>> resultado = new LinkedHashMap<>();
        for (final MarcadorEntity e : marcadores.findAllByOrderByTipoAscObligatorioDescMarcadorAsc()) {
            resultado.computeIfAbsent(e.getTipo(), k -> new java.util.ArrayList<>()).add(aDominio(e));
        }
        return resultado;
    }

    @Override
    public List<VersionPlantilla> versiones(final String tipo, final String idioma) {
        return plantillas.findByTipoAndIdiomaOrderByVersionDesc(tipo, idioma).stream().map(PlantillaRepositoryAdapter::aDominio).toList();
    }

    @Override
    public Map<String, List<VersionPlantilla>> versionesPorTipo(final String idioma) {
        final Map<String, List<VersionPlantilla>> resultado = new LinkedHashMap<>();
        for (final PlantillaEntity e : plantillas.findByIdiomaOrderByTipoAscVersionDesc(idioma)) {
            resultado.computeIfAbsent(e.getTipo(), k -> new java.util.ArrayList<>()).add(aDominio(e));
        }
        return resultado;
    }

    @Override
    public Optional<VersionPlantilla> activa(final String tipo, final String idioma) {
        return plantillas.findByTipoAndIdiomaAndActivaTrue(tipo, idioma).map(PlantillaRepositoryAdapter::aDominio);
    }

    @Override
    public Optional<VersionPlantilla> version(final String tipo, final String idioma, final int version) {
        return plantillas.findByTipoAndIdiomaAndVersion(tipo, idioma, version).map(PlantillaRepositoryAdapter::aDominio);
    }

    @Override
    @Transactional
    public VersionPlantilla crearYActivar(final String tipo, final String idioma, final String asunto, final String cuerpo,
                                          final String creadoPor, final Instant ahora) {
        // Serializa la creacion de versiones del mismo tipo para que dos administradores no calculen la misma version.
        entityManager.find(TipoNotificacionEntity.class, tipo, LockModeType.PESSIMISTIC_WRITE);
        final int siguiente = plantillas.maxVersion(tipo, idioma) + 1;
        final PlantillaEntity nueva = new PlantillaEntity(UUID.randomUUID(), tipo, idioma, siguiente, asunto, cuerpo, false, creadoPor, ahora);
        plantillas.saveAndFlush(nueva);
        plantillas.desactivarOtras(tipo, idioma, nueva.getId());
        final PlantillaEntity guardada = plantillas.findById(nueva.getId()).orElseThrow();
        guardada.setActiva(true);
        plantillas.saveAndFlush(guardada);
        return aDominio(guardada);
    }

    @Override
    @Transactional
    public Optional<VersionPlantilla> activar(final String tipo, final String idioma, final int version) {
        final Optional<PlantillaEntity> objetivo = plantillas.findByTipoAndIdiomaAndVersion(tipo, idioma, version);
        if (objetivo.isEmpty()) {
            return Optional.empty();
        }
        final UUID id = objetivo.get().getId();
        plantillas.desactivarOtras(tipo, idioma, id);
        final PlantillaEntity guardada = plantillas.findById(id).orElseThrow();
        guardada.setActiva(true);
        plantillas.saveAndFlush(guardada);
        return Optional.of(aDominio(guardada));
    }

    private static TipoNotificacion aDominio(final TipoNotificacionEntity e) {
        return new TipoNotificacion(e.getTipo(), e.getDescripcion(), e.isInterno());
    }

    private static Marcador aDominio(final MarcadorEntity e) {
        return new Marcador(e.getMarcador(), e.getDescripcion(), e.getEjemplo(), e.isObligatorio());
    }

    private static VersionPlantilla aDominio(final PlantillaEntity e) {
        return new VersionPlantilla(e.getId(), e.getTipo(), e.getIdioma(), e.getVersion(), e.getAsunto(), e.getCuerpo(),
                e.isActiva(), e.getCreadoPor(), e.getCreadoEn());
    }
}
