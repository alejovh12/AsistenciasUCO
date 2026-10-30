package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContactoDestinatarioJpaRepositoryTest {

    private EntityManager entityManager;
    private ContactoDestinatarioJpaRepository repository;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        repository = new ContactoDestinatarioJpaRepository(entityManager);
    }

    private StoredProcedureQuery mockSpQuery() {
        final StoredProcedureQuery sp = mock(StoredProcedureQuery.class);
        when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_contacto_notificacion")).thenReturn(sp);
        when(sp.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(sp);
        when(sp.setParameter(anyString(), any())).thenReturn(sp);
        return sp;
    }

    @Test
    @DisplayName("BE-C01: mapeo de contactos (idUsuario, correo, correoConfirmado) con tipos de UUID y booleanos heterogeneos; lista vacia o nula no consulta BD")
    void BE_C01_mapeo_de_contactos_con_uuid_y_booleanos_heterogeneos() {
        assertTrue(repository.consultarContactos(null).isEmpty());
        assertTrue(repository.consultarContactos(List.of()).isEmpty());
        verifyNoInteractions(entityManager);

        final StoredProcedureQuery sp = mockSpQuery();
        final UUID u1 = UUID.randomUUID();
        final UUID u2 = UUID.randomUUID();
        final UUID u3 = UUID.randomUUID();
        final Object[] canonicalOk = new Object[]{UUID.randomUUID(), "OK", "DBCODE=GEN_003|OK", Boolean.TRUE};
        final Object[] c1 = new Object[]{u1, "u1@uco.edu.co", Boolean.TRUE};
        final Object[] c2 = new Object[]{u2.toString(), "u2@uco.edu.co", 0};
        final Object[] c3 = new Object[]{u3.toString(), "u3@uco.edu.co", "true"};

        when(sp.execute()).thenReturn(true);
        when(sp.hasMoreResults()).thenReturn(true, false);
        when(sp.getResultList())
                .thenReturn(List.of((Object) canonicalOk))
                .thenReturn(List.of(c1, c2, c3));

        final List<ContactoDestinatario> contactos = repository.consultarContactos(List.of(u1, u2, u1, u3));
        assertEquals(3, contactos.size());
        assertEquals(new ContactoDestinatario(u1, "u1@uco.edu.co", true), contactos.get(0));
        assertEquals(new ContactoDestinatario(u2, "u2@uco.edu.co", false), contactos.get(1));
        assertEquals(new ContactoDestinatario(u3, "u3@uco.edu.co", true), contactos.get(2));

        final ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(sp).setParameter(eq("usuariosJson"), jsonCaptor.capture());
        final String json = jsonCaptor.getValue();
        assertTrue(json.contains(u1.toString()));
        assertTrue(json.contains(u2.toString()));
        assertTrue(json.contains(u3.toString()));
    }

    @Test
    @DisplayName("BE-C02: error del SP (exitoso=0) o PersistenceException lanza DatabaseOperationException")
    void BE_C02_error_sp_o_jpa_lanza_excepcion_transitoria() {
        final StoredProcedureQuery spError = mockSpQuery();
        final Object[] canonicalFail = new Object[]{UUID.randomUUID(), "Datos invalidos", "DBCODE=NTF_001|Invalido", Boolean.FALSE};
        when(spError.execute()).thenReturn(true);
        when(spError.getResultList()).thenReturn(List.of((Object) canonicalFail));

        assertThrows(DatabaseOperationException.class,
                () -> repository.consultarContactos(List.of(UUID.randomUUID())));

        // Caso sin segundo result set (hasMoreResults = false) con correlationId en contexto
        final UUID ctxCorr = UUID.randomUUID();
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(ctxCorr);
        try {
            final StoredProcedureQuery spVacio = mock(StoredProcedureQuery.class);
            when(entityManager.createStoredProcedureQuery("dbo.usp_consultar_contacto_notificacion")).thenReturn(spVacio);
            when(spVacio.registerStoredProcedureParameter(anyString(), any(), any())).thenReturn(spVacio);
            when(spVacio.setParameter(anyString(), any())).thenReturn(spVacio);
            when(spVacio.execute()).thenReturn(true);
            when(spVacio.hasMoreResults()).thenReturn(false);
            when(spVacio.getResultList()).thenReturn(List.of((Object) new Object[]{ctxCorr, "OK", "DBCODE=GEN_003|OK", 1}));

            assertTrue(repository.consultarContactos(List.of(UUID.randomUUID())).isEmpty());
        } finally {
            co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
        }

        when(entityManager.createStoredProcedureQuery(anyString())).thenThrow(new PersistenceException("DB down"));
        assertThrows(DatabaseOperationException.class,
                () -> repository.consultarContactos(List.of(UUID.randomUUID())));

        final ContactoDestinatario cd = new ContactoDestinatario(UUID.randomUUID(), "  a@uco.edu.co  ", false);
        assertEquals("a@uco.edu.co", cd.correo());
        assertFalse(cd.correoConfirmado());
    }
}
