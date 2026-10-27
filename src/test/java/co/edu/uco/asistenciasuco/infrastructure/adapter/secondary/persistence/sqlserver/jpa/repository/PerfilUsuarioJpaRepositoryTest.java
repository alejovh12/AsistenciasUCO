package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU006: EXEC del perfil con binding nombrado y agrupacion de vinculos (PR-01..PR-05). */
class PerfilUsuarioJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private PerfilUsuarioJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new PerfilUsuarioJpaRepository(entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] fila(final String rol, final UUID entidad, final Object activo) {
        return new Object[]{ACTOR, "CC ", "Cedula", 123, "Ana", null, "Perez", null, "Ana Perez", "ana@uco.edu.co", true, true,
                rol, entidad, rol == null ? null : "Ingenieria", rol == null ? null : "Facultad", activo};
    }

    @Test
    void PR01_ejecuta_el_sp_y_agrupa_los_vinculos() {
        final UUID programa = UUID.randomUUID();
        when(query.getResultList()).thenReturn(List.<Object>of(fila("ESTUDIANTE", programa, true), fila("DOCENTE", programa, false)));

        final var perfil = repository.consultarPerfil(ACTOR).orElseThrow();

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_perfil_usuario"));
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);
        assertEquals(ACTOR, perfil.idUsuario());
        assertEquals("CC", perfil.codigoTipoIdentificacion());
        assertEquals(123, perfil.numeroIdentificacion());
        assertNull(perfil.segundoNombre());
        assertEquals("ana@uco.edu.co", perfil.correo());
        assertTrue(perfil.correoConfirmado());
        assertTrue(perfil.estaActivo());
        assertEquals(2, perfil.vinculos().size());
        assertEquals("ESTUDIANTE", perfil.vinculos().getFirst().rol());
        assertEquals(programa, perfil.vinculos().getFirst().idEntidad());
        assertEquals("Facultad", perfil.vinculos().getFirst().detalle());
        assertFalse(perfil.vinculos().get(1).estaActivo());
    }

    @Test
    void PR02_fila_sin_vinculo_da_perfil_sin_vinculos() {
        final Object[] sinVinculo = fila(null, null, null);
        sinVinculo[3] = null;
        when(query.getResultList()).thenReturn(Collections.singletonList(sinVinculo));

        final var perfil = repository.consultarPerfil(ACTOR).orElseThrow();

        assertTrue(perfil.vinculos().isEmpty());
        assertNull(perfil.numeroIdentificacion());
    }

    @Test
    void PR03_usuario_inexistente_da_vacio() {
        when(query.getResultList()).thenReturn(Collections.emptyList());

        assertTrue(repository.consultarPerfil(ACTOR).isEmpty());
    }

    @Test
    void PR04_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.consultarPerfil(null));
        verify(entityManager, never()).createNativeQuery(anyString());
        assertThrows(NullPointerException.class, () -> new PerfilUsuarioJpaRepository(null));
    }

    @Test
    void PR05_fallo_tecnico_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));

        assertThrows(DatabaseOperationException.class, () -> repository.consultarPerfil(ACTOR));
    }
}
