package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort.Solicitante;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU177: el adaptador envia el SHA-256 del token (nunca el token) y traduce las filas de los SP de recuperacion. */
class RecuperacionClaveJpaRepositoryTest {

    private static final String TOKEN = "A".repeat(43);
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELACION = UUID.randomUUID();

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor procedureExecutor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final RecuperacionClaveJpaRepository repository = new RecuperacionClaveJpaRepository(entityManager, procedureExecutor);

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void consultar_solicitante_devuelve_el_usuario_y_su_nombre() {
        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[] {USUARIO.toString(), "ANA", "ana.perez@uco.edu.co"}));

        final Optional<Solicitante> solicitante = repository.consultarSolicitante("ana.perez@uco.edu.co");

        assertEquals(new Solicitante(USUARIO, "ANA"), solicitante.orElseThrow());
        verify(entityManager).createNativeQuery(RecuperacionClaveJpaRepository.SQL_SOLICITANTE);
        verify(query).setParameter("correo", "ana.perez@uco.edu.co");
        verify(query).setParameter("idCorrelacion", CORRELACION);
    }

    @Test
    void consultar_solicitante_sin_filas_devuelve_vacio_y_admite_nombre_nulo() {
        when(query.getResultList()).thenReturn(List.of());
        assertTrue(repository.consultarSolicitante("nadie@uco.edu.co").isEmpty());

        when(query.getResultList()).thenReturn(java.util.Collections.<Object>singletonList(new Object[] {USUARIO, null, "ana.perez@uco.edu.co"}));
        assertNull(repository.consultarSolicitante("ana.perez@uco.edu.co").orElseThrow().primerNombre());
    }

    @Test
    void consultar_usuario_por_token_envia_el_hash_y_lee_el_usuario() {
        when(query.getResultList()).thenReturn(List.<Object>of(USUARIO));

        assertEquals(USUARIO, repository.consultarUsuarioPorToken(TOKEN).orElseThrow());

        final ArgumentCaptor<Object> hash = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq("tokenHash"), hash.capture());
        assertArrayEquals(TokenHashHelper.sha256(TOKEN), (byte[]) hash.getValue());
        verify(entityManager).createNativeQuery(RecuperacionClaveJpaRepository.SQL_DUENO);
    }

    @Test
    void consultar_usuario_por_token_sin_filas_devuelve_vacio() {
        when(query.getResultList()).thenReturn(List.of());

        assertTrue(repository.consultarUsuarioPorToken(TOKEN).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consumir_ejecuta_el_sp_canonico_con_el_hash_y_la_correlacion() {
        repository.consumir(TOKEN);

        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(procedureExecutor).execute(
                eq(RecuperacionClaveJpaRepository.OP_CONSUMIR), eq(RecuperacionClaveJpaRepository.SQL_CONSUMIR),
                parametros.capture(), eq(CORRELACION));
        assertArrayEquals(TokenHashHelper.sha256(TOKEN), (byte[]) parametros.getValue().get("tokenHash"));
        assertEquals(CORRELACION, parametros.getValue().get("idCorrelacion"));
    }
}
