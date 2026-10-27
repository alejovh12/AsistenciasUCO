package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionSalienteJpaRepositoryTest {

    private JpaProcedureExecutor procedureExecutor;
    private EntityManager entityManager;
    private NotificacionSalienteJpaRepository repository;

    @BeforeEach
    void setUp() {
        procedureExecutor = mock(JpaProcedureExecutor.class);
        entityManager = mock(EntityManager.class);
        repository = new NotificacionSalienteJpaRepository(procedureExecutor, entityManager);
    }

    @Test
    @DisplayName("BE-J01: registrar invoca usp_registrar_notificacion_saliente con parametros exactos")
    void BE_J01_registrar_invoca_sp_con_parametros() {
        final UUID corrId = UUID.randomUUID();
        final RegistroNotificacion registro = new RegistroNotificacion(
                UUID.randomUUID(),
                "RETIRO_ESTUDIANTE_GRUPO",
                "Asunto",
                "Mensaje",
                "[{\"rol\":\"ESTUDIANTE\",\"idUsuario\":\"" + UUID.randomUUID() + "\"}]",
                "{\"grupoId\":\"123\"}",
                corrId,
                "trace-1",
                "span-1"
        );

        when(procedureExecutor.execute(anyString(), anyString(), anyMap(), any()))
                .thenReturn(new ProcedureResult(corrId, "OK", "OK", true));

        assertDoesNotThrow(() -> repository.registrar(registro));

        final ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(procedureExecutor).execute(
                eq("registrar_notificacion_saliente"),
                eq("EXEC dbo.usp_registrar_notificacion_saliente @tipo = :tipo, @asunto = :asunto, @mensaje = :mensaje, @destinatariosJson = :destinatariosJson, @datosJson = :datosJson, @idCorrelacion = :idCorrelacion, @traceId = :traceId, @spanId = :spanId"),
                captor.capture(),
                eq(corrId)
        );

        final Map<String, ?> params = captor.getValue();
        assertEquals("RETIRO_ESTUDIANTE_GRUPO", params.get("tipo"));
        assertEquals("Asunto", params.get("asunto"));
        assertEquals("Mensaje", params.get("mensaje"));
        assertEquals(corrId, params.get("idCorrelacion"));
        assertEquals("trace-1", params.get("traceId"));
        assertEquals("span-1", params.get("spanId"));
    }

    @Test
    @DisplayName("BE-J02: reservar mapea filas a NotificacionReservada")
    void BE_J02_reservar_mapea_filas() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        final UUID id = UUID.randomUUID();
        final Object[] fila = new Object[]{
                id,
                "TIPO_A",
                "Asunto A",
                "Mensaje A",
                "[{\"rol\":\"DOCENTE\",\"idUsuario\":\"" + UUID.randomUUID() + "\"}]",
                "{\"clave\":\"valor\"}",
                1,
                5,
                Instant.now(),
                "traceId123",
                "spanId123"
        };
        when(query.getResultList()).thenReturn(List.of((Object) fila));

        final List<NotificacionReservada> resultado = repository.reservar(10, Duration.ofSeconds(60), "instancia-1");

        assertFalse(resultado.isEmpty());
        final NotificacionReservada res = resultado.get(0);
        assertEquals(id, res.id());
        assertEquals("TIPO_A", res.tipo());
        assertEquals(1, res.intentos());
        assertEquals("traceId123", res.traceId());
    }

    @Test
    @DisplayName("BE-J03: depurar invoca executeWithExtras y devuelve eliminadas")
    void BE_J03_depurar_devuelve_eliminadas() {
        final UUID corr = UUID.randomUUID();
        final ProcedureResult procResult = new ProcedureResult(corr, "OK", "OK", true);
        final ProcedureRow row = new ProcedureRow(procResult, List.of(15));

        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(1)))
                .thenReturn(row);

        final int eliminadas = repository.depurar(30, 100);
        assertEquals(15, eliminadas);
    }
}
