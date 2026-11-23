package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.verificacion;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Emision del hash del token de verificacion: llamada al SP, cierre del EntityManager dedicado y rechazos del SP. */
class TokenVerificacionCorreoJpaRepositoryTest {

    private EntityManager entityManager;
    private EntityManager dedicado;
    private StoredProcedureQuery sp;
    private TokenVerificacionCorreoJpaRepository repositorio;
    private final UUID correlacion = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        entityManager = mock(EntityManager.class);
        dedicado = mock(EntityManager.class);
        sp = mock(StoredProcedureQuery.class);
        final EntityManagerFactory fabrica = mock(EntityManagerFactory.class);
        when(entityManager.getEntityManagerFactory()).thenReturn(fabrica);
        when(fabrica.createEntityManager()).thenReturn(dedicado);
        when(dedicado.isOpen()).thenReturn(true);
        when(dedicado.createStoredProcedureQuery("dbo.usp_emitir_token_verificacion_correo")).thenReturn(sp);
        when(sp.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(sp);
        when(sp.setParameter(anyString(), any())).thenReturn(sp);
        repositorio = new TokenVerificacionCorreoJpaRepository(entityManager);
        CorrelationIdContext.set(correlacion);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void registra_el_hash_con_el_sp_y_cierra_el_entity_manager_dedicado() {
        final UUID usuario = UUID.randomUUID();
        final byte[] hash = {1, 2, 3};
        when(sp.getResultList()).thenReturn(List.of((Object) new Object[] {correlacion, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE}));

        repositorio.registrar(usuario, hash);

        verify(sp).setParameter("idUsuario", usuario);
        verify(sp).setParameter("tokenHash", hash);
        verify(sp).execute();
        verify(dedicado).close();
    }

    @Test
    void un_rechazo_del_sp_se_traduce_y_aun_asi_cierra_el_entity_manager() {
        when(sp.getResultList()).thenReturn(List.of((Object) new Object[] {
                correlacion, "El correo ya fue verificado.", "DBCODE=VCE_005|ya verificado", Boolean.FALSE}));

        assertThrows(ConflictException.class, () -> repositorio.registrar(UUID.randomUUID(), new byte[] {1}));

        verify(dedicado).close();
    }

    @Test
    void sin_fabrica_usa_el_entity_manager_recibido_y_no_lo_cierra() {
        when(entityManager.getEntityManagerFactory()).thenReturn(null);
        when(entityManager.createStoredProcedureQuery("dbo.usp_emitir_token_verificacion_correo")).thenReturn(sp);
        when(sp.getResultList()).thenReturn(List.of((Object) new Object[] {correlacion, "OK", "DBCODE=GEN_003|OK", Boolean.TRUE}));

        repositorio.registrar(UUID.randomUUID(), new byte[] {1});

        verify(entityManager, never()).close();
        verify(sp).execute();
    }

    @Test
    void los_argumentos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> repositorio.registrar(null, new byte[] {1}));
        assertThrows(NullPointerException.class, () -> repositorio.registrar(UUID.randomUUID(), null));
        assertThrows(NullPointerException.class, () -> new TokenVerificacionCorreoJpaRepository(null));
        verify(sp, never()).execute();
        verify(dedicado, never()).close();
    }
}
