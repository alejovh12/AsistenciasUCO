package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU090: consulta del catalogo (HQL sobre uv_tipo_vinculacion, solo activos). */
class TipoVinculacionJpaRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final TypedQuery<?> typedQuery = mock(TypedQuery.class);
    private final TipoVinculacionJpaRepository repository = new TipoVinculacionJpaRepository(entityManager);

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new TipoVinculacionJpaRepository(null));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void consulta_solo_activos_ordenados() {
        when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.<Class>any())).thenReturn((TypedQuery) typedQuery);
        when(typedQuery.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarActivos().isEmpty());

        verify(entityManager).createQuery(contains("estaActivoTipoVinculacion"), org.mockito.ArgumentMatchers.<Class>any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void fallo_tecnico_se_traduce_a_error_de_operacion() {
        when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.<Class>any())).thenReturn((TypedQuery) typedQuery);
        when(typedQuery.getResultList()).thenThrow(new PersistenceException("caida"));

        assertThrows(DatabaseOperationException.class, repository::consultarActivos);
    }
}
