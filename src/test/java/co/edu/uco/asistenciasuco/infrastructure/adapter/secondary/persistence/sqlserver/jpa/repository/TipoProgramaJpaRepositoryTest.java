package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort.ActualizarTipoProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort.CambiarEstadoTipoProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort.CrearTipoProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvTipoProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TipoProgramaJpaRepositoryTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID CORRELATION_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final TipoProgramaJpaRepository repository = new TipoProgramaJpaRepository(mock(EntityManager.class), executor);

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_delega_el_comando_con_correlacion_y_ejecutor() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.crearTipoPrograma(new CrearTipoProgramaCommand("PRE", "pregrado", ACTOR));

        verify(executor).execute(
                eq(TipoProgramaJpaRepository.OP_CREAR),
                eq(TipoProgramaJpaRepository.SQL_CREAR),
                eq(Map.of("codigo", "PRE", "nombre", "pregrado", "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void actualizar_delega_el_comando_con_correlacion_y_ejecutor() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.actualizarTipoPrograma(new ActualizarTipoProgramaCommand(ID, "Nuevo", ACTOR));

        verify(executor).execute(
                eq(TipoProgramaJpaRepository.OP_ACTUALIZAR),
                eq(TipoProgramaJpaRepository.SQL_ACTUALIZAR),
                eq(Map.of("id", ID, "nombre", "Nuevo", "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void cambiar_estado_delega_el_estado_deseado() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.cambiarEstadoTipoPrograma(new CambiarEstadoTipoProgramaCommand(ID, false, ACTOR));

        verify(executor).execute(
                eq(TipoProgramaJpaRepository.OP_CAMBIAR_ESTADO),
                eq(TipoProgramaJpaRepository.SQL_CAMBIAR_ESTADO),
                eq(Map.of("id", ID, "estaActivo", false, "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void los_sql_ejecutan_los_sp_publicos() {
        assertTrue(TipoProgramaJpaRepository.SQL_CREAR.contains("EXEC dbo.usp_crear_tipo_programa"));
        assertTrue(TipoProgramaJpaRepository.SQL_ACTUALIZAR.contains("EXEC dbo.usp_actualizar_tipo_programa"));
        assertTrue(TipoProgramaJpaRepository.SQL_CAMBIAR_ESTADO.contains("EXEC dbo.usp_cambiar_estado_tipo_programa"));
    }

    @Test
    void sin_correlacion_activa_los_comandos_fallan() {
        final CrearTipoProgramaCommand command = new CrearTipoProgramaCommand("PRE", "pregrado", ACTOR);
        assertThrows(RuntimeException.class, () -> repository.crearTipoPrograma(command));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_mapea_las_filas_de_la_vista() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<UvTipoProgramaEntity> query = mock(TypedQuery.class);
        final UvTipoProgramaEntity row = mock(UvTipoProgramaEntity.class);
        when(entityManager.createQuery(anyString(), eq(UvTipoProgramaEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row));
        when(row.id()).thenReturn(ID);
        when(row.codigo()).thenReturn("PRE");
        when(row.nombre()).thenReturn("pregrado");
        when(row.estaActivoTipoPrograma()).thenReturn(false);

        final TipoProgramaProjection result = new TipoProgramaJpaRepository(entityManager, executor)
                .consultarTiposPrograma().get(0);

        assertEquals(ID, result.id());
        assertEquals("PRE", result.codigo());
        assertEquals("pregrado", result.nombre());
        assertFalse(result.estaActivo());
    }
}
