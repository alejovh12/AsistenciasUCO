package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort.ActualizarAreaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort.CambiarEstadoAreaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort.CrearAreaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAreaEntity;
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

class AreaJpaRepositoryCommandsTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID CORRELATION_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final AreaJpaRepository repository = new AreaJpaRepository(mock(EntityManager.class), executor);

    @AfterEach
    void tearDown() {
        CorrelationIdContext.clear();
    }

    @Test
    void crear_delega_el_comando_con_correlacion_y_ejecutor() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.crearArea(new CrearAreaCommand("CBA", "Ciencias Basicas", ACTOR));

        verify(executor).execute(
                eq(AreaJpaRepository.OP_CREAR_AREA),
                eq(AreaJpaRepository.SQL_CREAR_AREA),
                eq(Map.of("codigo", "CBA", "nombre", "Ciencias Basicas", "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void actualizar_delega_el_comando_con_correlacion_y_ejecutor() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.actualizarArea(new ActualizarAreaCommand(ID, "Nuevo", ACTOR));

        verify(executor).execute(
                eq(AreaJpaRepository.OP_ACTUALIZAR_AREA),
                eq(AreaJpaRepository.SQL_ACTUALIZAR_AREA),
                eq(Map.of("id", ID, "nombre", "Nuevo", "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void cambiar_estado_delega_el_estado_deseado() {
        CorrelationIdContext.set(CORRELATION_ID);

        repository.cambiarEstadoArea(new CambiarEstadoAreaCommand(ID, false, ACTOR));

        verify(executor).execute(
                eq(AreaJpaRepository.OP_CAMBIAR_ESTADO_AREA),
                eq(AreaJpaRepository.SQL_CAMBIAR_ESTADO_AREA),
                eq(Map.of("id", ID, "estaActiva", false, "idCorrelacion", CORRELATION_ID, "idUsuarioEjecutor", ACTOR)),
                eq(CORRELATION_ID));
    }

    @Test
    void los_sql_ejecutan_los_sp_publicos() {
        assertTrue(AreaJpaRepository.SQL_CREAR_AREA.contains("EXEC dbo.usp_crear_area"));
        assertTrue(AreaJpaRepository.SQL_ACTUALIZAR_AREA.contains("EXEC dbo.usp_actualizar_area"));
        assertTrue(AreaJpaRepository.SQL_CAMBIAR_ESTADO_AREA.contains("EXEC dbo.usp_cambiar_estado_area"));
    }

    @Test
    void sin_correlacion_activa_los_comandos_fallan() {
        final CrearAreaCommand command = new CrearAreaCommand("CBA", "Ciencias Basicas", ACTOR);
        assertThrows(RuntimeException.class, () -> repository.crearArea(command));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consultar_mapea_las_filas_de_la_vista() {
        final EntityManager entityManager = mock(EntityManager.class);
        final TypedQuery<UvAreaEntity> query = mock(TypedQuery.class);
        final UvAreaEntity row = mock(UvAreaEntity.class);
        when(entityManager.createQuery(anyString(), eq(UvAreaEntity.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(row));
        when(row.id()).thenReturn(ID);
        when(row.codigo()).thenReturn("CBA");
        when(row.nombre()).thenReturn("Ciencias Basicas");
        when(row.estaActivaArea()).thenReturn(false);

        final AreaProjection result = new AreaJpaRepository(entityManager, executor)
                .consultarAreas().get(0);

        assertEquals(ID, result.id());
        assertEquals("CBA", result.codigo());
        assertEquals("Ciencias Basicas", result.nombre());
        assertFalse(result.estaActiva());
    }
}
