package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.List;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para resolver mensajes desde SQL Server
 * (vistas uv_mensaje_usuario y uv_mensaje_tecnico, JPA-only desde LB-008 JPA-06).
 *
 * <p>Mantiene una cache en memoria para evitar consultas recurrentes a la base de datos.
 * Solo se cachean resultados positivos: codigos inexistentes o contenido nulo no se cachean.</p>
 */
@Repository
@ConditionalOnProperty(prefix = "app.adapters.message-catalog", name = "provider", havingValue = "sqlserver")
public class MessageCatalogJpaRepository implements MessageCatalogPort {

    static final String HQL_MENSAJE_USUARIO = "select m.contenido from UvMensajeUsuarioEntity m where m.codigo = :codigo";
    static final String HQL_MENSAJE_TECNICO = "select m.contenido from UvMensajeTecnicoEntity m where m.codigo = :codigo";
    private final EntityManager entityManager;
    private final Map<String, String> userMessageCache = new ConcurrentHashMap<>();
    private final Map<String, String> technicalMessageCache = new ConcurrentHashMap<>();

    public MessageCatalogJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de mensajes es obligatorio.");
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
        if (userMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(userMessageCache.get(cleanCode));
        }

        try {
            final Optional<String> contenido = buscarMensajeUsuario(cleanCode);
            contenido.ifPresent(valor -> userMessageCache.put(cleanCode, valor));
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
        if (technicalMessageCache.containsKey(cleanCode)) {
            return Optional.ofNullable(technicalMessageCache.get(cleanCode));
        }

        try {
            final Optional<String> contenido = buscarMensajeTecnico(cleanCode);
            contenido.ifPresent(valor -> technicalMessageCache.put(cleanCode, valor));
            return contenido;
        } catch (final Exception e) {
            throw new MessageCatalogException("Error al consultar mensaje tecnico para el codigo: " + code, e);
        }
    }

    private String formatMessage(final String pattern, final Object... args) {
        if (args == null || args.length == 0 || pattern == null || pattern.isBlank()) {
            return pattern;
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


