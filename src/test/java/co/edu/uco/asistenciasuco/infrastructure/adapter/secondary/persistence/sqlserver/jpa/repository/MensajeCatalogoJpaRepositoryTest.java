package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INT-003: adaptador JPA de usp_sincronizar_mensaje_usuario (tupla canonica + codigo, huboCambio, fecha, existeMensaje). */
class MensajeCatalogoJpaRepositoryTest {

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private final MensajeCatalogoJpaRepository repository = new MensajeCatalogoJpaRepository(entityManager);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private void fila(final boolean estado, final String tecnico, final Boolean huboCambio, final Boolean existe) {
        final Object[] columnas = {correlacion, estado ? "Mensaje actualizado(a) exitosamente." : "Error", tecnico, estado,
                estado ? "VAL_001" : null, huboCambio, estado ? Timestamp.valueOf("2026-11-15 00:00:00") : null, existe};
        final List<Object> filas = new java.util.ArrayList<>();
        filas.add(columnas);
        when(query.getResultList()).thenReturn(filas);
    }

    @Test
    void texto_distinto_es_actualizado_y_envia_codigo_contenido_y_correlacion() {
        fila(true, "GEN_005", true, true);

        assertEquals(ResultadoSincronizacion.ACTUALIZADO, repository.sincronizarMensajeUsuario("VAL_001", "Texto"));
        verify(entityManager).createNativeQuery(MensajeCatalogoJpaRepository.SQL_SINCRONIZAR);
        verify(query).setParameter("codigo", "VAL_001");
        verify(query).setParameter("contenido", "Texto");
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void mismo_texto_es_sin_cambios() {
        fila(true, "GEN_005", false, true);
        assertEquals(ResultadoSincronizacion.SIN_CAMBIOS, repository.sincronizarMensajeUsuario("VAL_001", "Texto"));
    }

    @Test
    void codigo_que_la_bd_no_tiene_es_no_existe_y_no_lanza() {
        fila(false, "GEN_001", null, false);
        assertEquals(ResultadoSincronizacion.NO_EXISTE, repository.sincronizarMensajeUsuario("UI_001", "Texto"));
    }

    @Test
    void otros_rechazos_se_traducen_con_el_validador_canonico() {
        fila(false, "[SYS_001] error", null, null);
        assertThrows(RuntimeException.class, () -> repository.sincronizarMensajeUsuario("VAL_001", "Texto"));

        fila(false, "[GEN_001] otro", null, true);
        assertThrows(RuntimeException.class, () -> repository.sincronizarMensajeUsuario("VAL_001", "Texto"));
    }

    @Test
    void fallo_de_persistencia_es_error_de_base_de_datos() {
        when(query.getResultList()).thenThrow(new PersistenceException("caida"));
        assertThrows(DatabaseOperationException.class, () -> repository.sincronizarMensajeUsuario("VAL_001", "Texto"));
    }

    @Test
    void sin_correlacion_no_ejecuta() {
        CorrelationIdContext.clear();
        assertThrows(RuntimeException.class, () -> repository.sincronizarMensajeUsuario("VAL_001", "Texto"));
    }

    @Test
    void entity_manager_obligatorio() {
        assertTrue(assertThrows(NullPointerException.class, () -> new MensajeCatalogoJpaRepository(null))
                .getMessage().contains("EntityManager"));
    }
}
