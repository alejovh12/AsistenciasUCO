package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;

import java.util.Optional;

/**
 * Harness exclusivo de src/test para el oráculo de paridad de catálogos JPA-06. NO es un repository
 * productivo ni un {@code @Repository}: solo agrupa los dos repositorios productivos
 * ({@link MessageCatalogJpaRepository} y {@link ParameterCatalogJpaRepository}) para comparar los tres
 * catálogos en una sola aserción. Producción conserva dos repositorios independientes por selector de provider.
 */
final class CatalogJpaParityHarness {

    private final MessageCatalogJpaRepository messages;
    private final ParameterCatalogJpaRepository parameters;

    CatalogJpaParityHarness(final EntityManager entityManager) {
        this.messages = new MessageCatalogJpaRepository(entityManager);
        this.parameters = new ParameterCatalogJpaRepository(entityManager);
    }

    Optional<String> buscarMensajeUsuario(final String codigo) {
        return messages.findUserMessage(codigo);
    }

    /**
     * Contrato publico {@code getTechnicalMessage}: sin resolucion devuelve el propio codigo como fallback.
     * Se traduce a Optional vacio para comparar con el oraculo JDBC; el fixture usa contenidos distintos al codigo.
     */
    Optional<String> buscarMensajeTecnico(final String codigo) {
        final String resuelto = messages.getTechnicalMessage(codigo);
        return resuelto.equals(codigo) ? Optional.empty() : Optional.of(resuelto);
    }

    Optional<String> buscarParametro(final String grupo, final String clave) {
        return parameters.getParameter(grupo, clave);
    }
}
