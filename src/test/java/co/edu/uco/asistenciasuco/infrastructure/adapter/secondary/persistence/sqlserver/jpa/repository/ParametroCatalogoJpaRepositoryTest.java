package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.Operacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ParametroCatalogoRepositoryPort.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INT-004: traduccion de la fila de usp_sincronizar_parametro_catalogo_externo (contrato SP: 4 canonicas + 5 columnas). */
class ParametroCatalogoJpaRepositoryTest {

    private static final Instant OCURRIDO = Instant.parse("2026-11-22T12:00:00Z");

    private EntityManager entityManager;
    private Query consulta;
    private ParametroCatalogoJpaRepository repositorio;
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        entityManager = mock(EntityManager.class);
        consulta = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(consulta);
        repositorio = new ParametroCatalogoJpaRepository(entityManager);
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private void filaDelSp(final boolean estado, final String resultado) {
        final Object[] fila = {correlacion, "mensaje", "tecnico", estado, resultado, "G", "K", estado, null};
        when(consulta.getResultList()).thenReturn(List.<Object>of((Object) fila));
    }

    @Test
    void envia_los_parametros_del_sp_con_la_hora_utc_y_traduce_cada_resultado() {
        for (final ResultadoSincronizacion esperado : List.of(ResultadoSincronizacion.APLICADO, ResultadoSincronizacion.SIN_CAMBIOS,
                ResultadoSincronizacion.DUPLICADO, ResultadoSincronizacion.OBSOLETO, ResultadoSincronizacion.DESCONOCIDO)) {
            filaDelSp(true, esperado.name());
            assertEquals(esperado, repositorio.sincronizarParametroExterno("G", "K", "v", Operacion.UPSERT, "evt-1", OCURRIDO));
        }
        verify(consulta, org.mockito.Mockito.atLeastOnce()).setParameter("grupo", "G");
        verify(consulta, org.mockito.Mockito.atLeastOnce()).setParameter("operacion", "UPSERT");
        verify(consulta, org.mockito.Mockito.atLeastOnce()).setParameter("idEvento", "evt-1");
        verify(consulta, org.mockito.Mockito.atLeastOnce()).setParameter("ocurridoEnUtc", LocalDateTime.of(2026, 11, 22, 12, 0));
        verify(consulta, org.mockito.Mockito.atLeastOnce()).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void un_valor_invalido_es_rechazado_sin_excepcion() {
        filaDelSp(false, "RECHAZADO");

        assertEquals(ResultadoSincronizacion.RECHAZADO,
                repositorio.sincronizarParametroExterno("G", "K", "abc", Operacion.UPSERT, null, OCURRIDO));
    }

    @Test
    void un_rechazo_sin_resultado_es_un_fallo_reintentable() {
        filaDelSp(false, null);

        assertThrows(RuntimeException.class,
                () -> repositorio.sincronizarParametroExterno("G", "K", "v", Operacion.DELETE, null, OCURRIDO));
    }

    @Test
    void un_resultado_desconocido_con_exito_es_un_error_de_contrato() {
        filaDelSp(true, "INVENTADO");

        assertThrows(RuntimeException.class,
                () -> repositorio.sincronizarParametroExterno("G", "K", "v", Operacion.UPSERT, null, OCURRIDO));
    }

    @Test
    void un_error_de_persistencia_se_traduce_a_error_de_base_de_datos() {
        when(consulta.getResultList()).thenThrow(new PersistenceException("sql caido"));

        assertThrows(RuntimeException.class,
                () -> repositorio.sincronizarParametroExterno("G", "K", "v", Operacion.UPSERT, null, OCURRIDO));
    }
}
