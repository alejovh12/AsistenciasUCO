package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador secundario para leer parametros desde SQL Server (vista uv_parametro, JPA-only desde LB-008 JPA-06).
 *
 * <p>Permite operar con los parametros institucionales persistidos en base de datos.
 * Incorpora cache en memoria para minimizar accesos repetitivos a la base de datos.
 * Solo se cachean valores positivos: parametros inexistentes o valor nulo no se cachean.
 * VAL-oleada7: las entradas vencen tras {@code app.adapters.sql-catalog-cache.ttl} para que todas las replicas tomen un
 * valor editado (la invalidacion explicita solo llega a la replica que atendio la edicion).</p>
 */
@Repository
@ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "sqlserver")
// CULM-001 A.2-6: proveedor real; los consumidores reciben el decorador con valores por defecto (DEC-018).
@Qualifier("parameterCatalogDelegado")
public class ParameterCatalogJpaRepository implements ParameterCatalogPort,
        co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog {

    static final String HQL_PARAMETRO = "select p.valor from UvParametroEntity p where p.grupo = :grupo and p.clave = :clave";
    private final EntityManager entityManager;
    private final CacheConVencimiento<ParameterKey, String> cache;

    /**
     * Structural key: concatenating group + "::" + key is ambiguous when either
     * identifier contains the separator (e.g., "a::b"/"c" vs "a"/"b::c").
     */
    private record ParameterKey(String group, String key) { }

    public ParameterCatalogJpaRepository(final EntityManager entityManager) {
        this(entityManager, Clock.systemUTC(), CacheConVencimiento.TTL_POR_DEFECTO);
    }

    @Autowired
    public ParameterCatalogJpaRepository(
            final EntityManager entityManager,
            @Value("${app.adapters.sql-catalog-cache.ttl:60s}") final Duration ttl
    ) {
        this(entityManager, Clock.systemUTC(), ttl);
    }

    ParameterCatalogJpaRepository(final EntityManager entityManager, final Clock reloj, final Duration ttl) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de parametros es obligatorio.");
        this.cache = new CacheConVencimiento<>(reloj, ttl);
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        final String normalizedGroup = group.trim();
        final String normalizedKey = key.trim();
        final ParameterKey cacheKey = new ParameterKey(normalizedGroup, normalizedKey);
        final Optional<String> cached = cache.obtener(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            final Optional<String> valor = buscarParametro(normalizedGroup, normalizedKey);
            valor.ifPresent(contenido -> cache.guardar(cacheKey, contenido));
            return valor;
        } catch (final Exception e) {
            throw new ParameterCatalogException("Error al consultar parametro en base de datos [" + group + ":" + key + "].", e);
        }
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key)
                .orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String rawValue = getRequiredParameter(group, key);
        if (targetType == String.class) {
            return (T) rawValue;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(rawValue.trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(rawValue.trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(rawValue.trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(rawValue.trim());
        }
        throw new IllegalArgumentException("Tipo no soportado: " + targetType.getName());
    }

    /** CULM-001 D.3: tras editar un parametro, la siguiente lectura vuelve a la vista uv_parametro. */
    @Override
    public void invalidateParameter(final String group, final String key) {
        if (group != null && key != null) {
            cache.invalidar(new ParameterKey(group.trim(), key.trim()));
        }
    }

    @Override
    public void invalidateAll() {
        cache.invalidarTodo();
    }

    private Optional<String> buscarParametro(final String grupo, final String clave) {
        return primerContenido(entityManager.createQuery(HQL_PARAMETRO, String.class)
                .setParameter("grupo", grupo).setParameter("clave", clave).setMaxResults(1).getResultList());
    }

    private static Optional<String> primerContenido(final List<String> resultados) {
        return resultados.isEmpty() ? Optional.empty() : Optional.ofNullable(resultados.getFirst());
    }
}


