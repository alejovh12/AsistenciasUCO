package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogosSistemaJpaRepositoryTest {

    private static final UUID ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final CatalogosSistemaJpaRepository repository = new CatalogosSistemaJpaRepository(entityManager);

    @Test
    void constructor_rechaza_entity_manager_nulo() {
        assertThrows(NullPointerException.class, () -> new CatalogosSistemaJpaRepository(null));
    }

    @Test
    void lee_categorias_de_la_vista() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{ID, "MED", "Médico", "Desc", true}));
        final var filas = repository.consultarCategoriasJustificacion();
        verify(entityManager).createNativeQuery(contains("uv_categoria_justificacion"));
        assertEquals(1, filas.size());
        assertEquals(ID, filas.get(0).id());
        assertEquals("MED", filas.get(0).codigo());
        assertEquals("Médico", filas.get(0).nombre());
        assertEquals("Desc", filas.get(0).descripcion());
        assertEquals(true, filas.get(0).requiereSoporte());
    }

    @Test
    void lee_motivos_de_la_vista_con_estado() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{ID, "OTRO", "Otro", 5, false}));
        final var filas = repository.consultarMotivosCancelacion();
        verify(entityManager).createNativeQuery(contains("uv_motivo_cancelacion_sesion"));
        assertEquals(1, filas.size());
        assertEquals("OTRO", filas.get(0).codigo());
        assertEquals(5, filas.get(0).orden());
        assertEquals(false, filas.get(0).estaActivo());
    }

    @Test
    void fallo_de_lectura_se_traduce() {
        when(entityManager.createNativeQuery(anyString())).thenThrow(new PersistenceException("fallo"));
        assertThrows(DatabaseOperationException.class, repository::consultarCategoriasJustificacion);
        assertThrows(DatabaseOperationException.class, repository::consultarMotivosCancelacion);
    }

    @Test
    void listas_vacias() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        assertEquals(0, repository.consultarCategoriasJustificacion().size());
        assertEquals(0, repository.consultarMotivosCancelacion().size());
    }
}
