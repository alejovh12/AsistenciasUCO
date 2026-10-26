package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityConflictException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort.ExternalIdentityLink;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El EntityManager y el ejecutor simulados no certifican SQL Server: ver {@code ExternalIdentityLinkJpaRepositoryIT}. */
class ExternalIdentityLinkJpaRepositoryTest {

    private static final String ISSUER = "http://idp.test/realms/r";
    private static final String SUBJECT = "sub-1";
    private static final UUID USER = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID CORRELATION = UUID.fromString("11111111-2222-4333-8444-555555555555");

    private final EntityManager manager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final ExternalIdentityLinkJpaRepository repository = new ExternalIdentityLinkJpaRepository(manager, executor);

    @AfterEach
    void clear() {
        CorrelationIdContext.clear();
    }

    @SuppressWarnings("unchecked")
    private void rows(final List<Object[]> result) {
        final TypedQuery<Object[]> query = mock(TypedQuery.class, RETURNS_SELF);
        when(query.getResultList()).thenReturn(result);
        when(manager.createQuery(ExternalIdentityLinkJpaRepository.HQL_VINCULO, Object[].class)).thenReturn(query);
    }

    @Test
    void find_devuelve_vacio_el_vinculo_vigente_o_el_revocado() {
        rows(List.of());
        assertEquals(Optional.empty(), repository.find(ISSUER, SUBJECT));

        rows(List.<Object[]>of(new Object[] {USER, Boolean.TRUE}));
        assertEquals(new ExternalIdentityLink(USER, true), repository.find(ISSUER, SUBJECT).orElseThrow());

        rows(List.<Object[]>of(new Object[] {USER, Boolean.FALSE}));
        assertFalse(repository.find(ISSUER, SUBJECT).orElseThrow().vigente());
    }

    @Test
    void find_no_elige_una_fila_si_hubiera_duplicados() {
        rows(List.of(new Object[] {USER, Boolean.TRUE}, new Object[] {USER, Boolean.TRUE}));

        assertThrows(DatabaseOperationException.class, () -> repository.find(ISSUER, SUBJECT));
    }

    @Test
    void find_traduce_fallos_de_persistencia() {
        when(manager.createQuery(ExternalIdentityLinkJpaRepository.HQL_VINCULO, Object[].class))
                .thenThrow(new PersistenceException("boom"));

        assertThrows(DatabaseOperationException.class, () -> repository.find(ISSUER, SUBJECT));
    }

    @Test
    void link_invoca_el_sp_con_la_correlacion_del_contexto_y_reporta_si_creo_el_vinculo() {
        CorrelationIdContext.set(CORRELATION);
        rows(List.of());

        assertTrue(repository.link(ISSUER, SUBJECT, USER, "persona@uco.edu.co"));

        verify(executor).execute(eq(ExternalIdentityLinkJpaRepository.OP_VINCULAR),
                eq(ExternalIdentityLinkJpaRepository.SQL_VINCULAR), anyMap(), eq(CORRELATION));
    }

    @Test
    void link_idempotente_cuando_el_vinculo_vigente_ya_era_del_mismo_usuario() {
        rows(List.<Object[]>of(new Object[] {USER, Boolean.TRUE}));

        assertFalse(repository.link(ISSUER, SUBJECT, USER, "persona@uco.edu.co"));
    }

    @Test
    void link_genera_correlacion_si_el_contexto_aun_no_existe_y_traduce_conflictos() {
        rows(List.of());
        doThrow(new ConflictException("duplicado")).when(executor)
                .execute(any(), any(), anyMap(), any());

        assertThrows(ExternalIdentityConflictException.class,
                () -> repository.link(ISSUER, SUBJECT, USER, "persona@uco.edu.co"));
    }

    @Test
    void revoke_invoca_el_sp_de_revocacion() {
        CorrelationIdContext.set(CORRELATION);

        repository.revoke(ISSUER, SUBJECT);

        verify(executor).execute(eq(ExternalIdentityLinkJpaRepository.OP_REVOCAR),
                eq(ExternalIdentityLinkJpaRepository.SQL_REVOCAR), anyMap(), eq(CORRELATION));
    }
}
