package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Adaptador secundario para resolver mensajes desde SQL Server
 * (vistas uv_mensaje_usuario y uv_mensaje_tecnico, JPA-only desde LB-008 JPA-06).
 *
 * <p>Mantiene una cache en memoria para evitar consultas recurrentes a la base de datos.
 * Solo se cachean resultados positivos: codigos inexistentes o contenido nulo no se cachean.
 * VAL-oleada7: las entradas vencen tras {@code app.adapters.sql-catalog-cache.ttl}, asi un mensaje editado llega a todas
 * las replicas sin reiniciarlas.</p>
 */
@Repository
@ConditionalOnProperty(prefix = "app.adapters.message-catalog", name = "provider", havingValue = "sqlserver")
public class MessageCatalogJpaRepository implements MessageCatalogPort {

    static final String HQL_MENSAJE_USUARIO = "select m.contenido from UvMensajeUsuarioEntity m where m.codigo = :codigo";
    static final String HQL_MENSAJE_TECNICO = "select m.contenido from UvMensajeTecnicoEntity m where m.codigo = :codigo";
    private final EntityManager entityManager;
    private final CacheConVencimiento<String, String> userMessageCache;
    private final CacheConVencimiento<String, String> technicalMessageCache;

    public MessageCatalogJpaRepository(final EntityManager entityManager) {
        this(entityManager, Clock.systemUTC(), CacheConVencimiento.TTL_POR_DEFECTO);
    }

    @Autowired
    public MessageCatalogJpaRepository(
            final EntityManager entityManager,
            @Value("${app.adapters.sql-catalog-cache.ttl:60s}") final Duration ttl
    ) {
        this(entityManager, Clock.systemUTC(), ttl);
    }

    MessageCatalogJpaRepository(final EntityManager entityManager, final Clock reloj, final Duration ttl) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de mensajes es obligatorio.");
        this.userMessageCache = new CacheConVencimiento<>(reloj, ttl);
        this.technicalMessageCache = new CacheConVencimiento<>(reloj, ttl);
    }

    @Override
    public String getUserMessage(final String code, final Object... args) {
        final String template = findUserMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public String getTechnicalMessage(final String code, final Object... args) {
        final String template = findTechnicalMessage(code).orElse(code);
        return formatMessage(template, args);
    }

    @Override
    public Optional<String> findUserMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        final Optional<String> enCache = userMessageCache.obtener(cleanCode);
        if (enCache.isPresent()) {
            return enCache;
        }

        try {
            final Optional<String> contenido = buscarMensajeUsuario(cleanCode);
            contenido.ifPresent(valor -> userMessageCache.guardar(cleanCode, valor));
            return contenido;
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje de usuario para el codigo: " + code, e);
        }
    }

    private Optional<String> findTechnicalMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        final String cleanCode = code.trim();
        final Optional<String> enCache = technicalMessageCache.obtener(cleanCode);
        if (enCache.isPresent()) {
            return enCache;
        }

        try {
            final Optional<String> contenido = buscarMensajeTecnico(cleanCode);
            contenido.ifPresent(valor -> technicalMessageCache.guardar(cleanCode, valor));
            return contenido;
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje tecnico para el codigo: " + code, e);
        }
    }

    /**
     * Acepta los dos formatos del catalogo, igual que los adaptadores de Azure y de MessageUcoLab: marcadores
     * secuenciales {@code {}} (los que usan las semillas de la BD y {@code usp_obtener_mensaje_catalogo}) y marcadores
     * indexados {@code {0}}.
     *
     * <p>CERT-CULM001 (HU026C): antes solo se aplicaba {@link MessageFormat}, que no entiende {@code {}} y devolvia la
     * plantilla sin sustituir. Por eso las alertas del estudiante salian como "Llevas {}% de inasistencia en {}".</p>
     */
    private String formatMessage(final String pattern, final Object... args) {
        if (args == null || args.length == 0 || pattern == null || pattern.isBlank()) {
            return pattern;
        }
        if (pattern.contains("{}")) {
            String texto = pattern;
            for (final Object arg : args) {
                texto = texto.replaceFirst("\\{\\}", Matcher.quoteReplacement(String.valueOf(arg)));
            }
            return texto;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (final Exception e) {
            return pattern;
        }
    }

    private Optional<String> buscarMensajeUsuario(final String codigo) {
        return primerContenido(entityManager.createQuery(HQL_MENSAJE_USUARIO, String.class)
                .setParameter("codigo", codigo).setMaxResults(1).getResultList());
    }

    private Optional<String> buscarMensajeTecnico(final String codigo) {
        return primerContenido(entityManager.createQuery(HQL_MENSAJE_TECNICO, String.class)
                .setParameter("codigo", codigo).setMaxResults(1).getResultList());
    }

    private static Optional<String> primerContenido(final List<String> resultados) {
        return resultados.isEmpty() ? Optional.empty() : Optional.ofNullable(resultados.getFirst());
    }
}


