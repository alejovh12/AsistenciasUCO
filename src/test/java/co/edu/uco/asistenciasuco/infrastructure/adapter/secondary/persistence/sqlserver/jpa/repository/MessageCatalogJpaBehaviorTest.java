package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * QUALITY-PR15 JPA04: message contract, two catalogs and positive cache.
 * Only adapter behavior; SQL Server real is covered by CatalogJpaParityIT.
 */
class MessageCatalogJpaBehaviorTest {
    private final EntityManager manager = mock(EntityManager.class);

    @SuppressWarnings("unchecked")
    private TypedQuery<String> queryReturning(final List<String> rows) {
        final TypedQuery<String> query = mock(TypedQuery.class);
        when(manager.createQuery(anyString(), eq(String.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.setMaxResults(1)).thenReturn(query);
        when(query.getResultList()).thenReturn(rows);
        return query;
    }

    @Test
    void codigo_nulo_o_en_blanco_no_toca_entity_manager() {
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        assertEquals(Optional.empty(), catalog.findUserMessage(null));
        assertEquals(Optional.empty(), catalog.findUserMessage(" "));
        verifyNoInteractions(manager);
    }

    @Test
    void codigo_se_normaliza_y_el_mensaje_encontrado_se_cachea() {
        final TypedQuery<String> query = queryReturning(List.of("Sesión creada"));
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        assertEquals(Optional.of("Sesión creada"), catalog.findUserMessage(" SES_001 "));
        assertEquals(Optional.of("Sesión creada"), catalog.findUserMessage("SES_001"));
        verify(query).setParameter("codigo", "SES_001");
        verify(query).setMaxResults(1);
        verify(manager, times(1)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void codigo_ausente_es_fallback_visible_y_no_se_cachea_el_miss() {
        final TypedQuery<String> query = queryReturning(List.of());
        when(query.getResultList()).thenReturn(List.of(), List.of("Ahora existe"));
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        assertEquals("SES_MISSING", catalog.getUserMessage("SES_MISSING"));
        assertEquals("Ahora existe", catalog.getUserMessage("SES_MISSING"));
        verify(manager, times(2)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void mensaje_con_argumentos_se_formatea_sin_perder_parametros() {
        queryReturning(List.of("El grupo {0} tiene {1} cupos"));
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        assertEquals("El grupo A tiene 3 cupos", catalog.getUserMessage("GRP_001", "A", 3));
    }

    @Test
    void mensaje_sin_argumentos_conserva_la_plantilla_original() {
        queryReturning(List.of("Sesión {0}"));
        assertEquals("Sesión {0}", new MessageCatalogJpaRepository(manager).getUserMessage("SES_001"));
    }

    @Test
    void consultas_de_usuario_y_tecnicas_son_independientes() {
        final TypedQuery<String> user = mock(TypedQuery.class);
        final TypedQuery<String> tech = mock(TypedQuery.class);
        when(manager.createQuery(contains("UvMensajeUsuarioEntity"), eq(String.class))).thenReturn(user);
        when(manager.createQuery(contains("UvMensajeTecnicoEntity"), eq(String.class))).thenReturn(tech);
        when(user.setParameter(anyString(), any())).thenReturn(user);
        when(tech.setParameter(anyString(), any())).thenReturn(tech);
        when(user.setMaxResults(1)).thenReturn(user);
        when(tech.setMaxResults(1)).thenReturn(tech);
        when(user.getResultList()).thenReturn(List.of("Mensaje público"));
        when(tech.getResultList()).thenReturn(List.of("Detalle técnico"));
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        assertEquals("Mensaje público", catalog.getUserMessage("ERR_1"));
        assertEquals("Detalle técnico", catalog.getTechnicalMessage("ERR_1"));
        assertEquals("Mensaje público", catalog.getUserMessage("ERR_1"));
        assertEquals("Detalle técnico", catalog.getTechnicalMessage("ERR_1"));
        verify(manager, times(2)).createQuery(anyString(), eq(String.class));
    }

    @Test
    void fallo_sql_no_se_convierta_en_mensaje_aparentemente_exitoso() {
        when(manager.createQuery(anyString(), eq(String.class))).thenThrow(new PersistenceException("db down"));
        final MessageCatalogJpaRepository catalog = new MessageCatalogJpaRepository(manager);
        final MessageCatalogPort.MessageCatalogException error = assertThrows(
                MessageCatalogPort.MessageCatalogException.class,
                () -> catalog.getUserMessage("ERR_1"));
        assertInstanceOf(PersistenceException.class, error.getCause());
    }
}
