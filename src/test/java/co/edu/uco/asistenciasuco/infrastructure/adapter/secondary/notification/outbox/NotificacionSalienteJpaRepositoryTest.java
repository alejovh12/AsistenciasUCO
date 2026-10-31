package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

        final ProcedureResult procResult = mock(ProcedureResult.class);
        when(procResult.getIdCorrelacion()).thenReturn(corrId);
        when(procResult.getEstadoResultado()).thenReturn(true);
        when(procResult.getMensajeUsuarioResultado()).thenReturn("OK");
        when(procResult.getMensajeTecnicoResultado()).thenReturn("OK");

        when(procedureExecutor.execute(anyString(), anyString(), anyMap(), any()))
                .thenReturn(procResult);

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
        final UUID corrId = UUID.randomUUID();
        final Instant creadoEn = Instant.now();
        final Object[] fila = {
                id,
                "TIPO_A",
                "Asunto A",
                "Mensaje A",
                "[{\"rol\":\"DOCENTE\",\"idUsuario\":\"" + UUID.randomUUID() + "\"}]",
                "{\"clave\":\"valor\"}",
                1,
                creadoEn,
                corrId,
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
        final ProcedureResult procResult = mock(ProcedureResult.class);
        when(procResult.getIdCorrelacion()).thenReturn(corr);
        when(procResult.getEstadoResultado()).thenReturn(true);
        when(procResult.getMensajeUsuarioResultado()).thenReturn("OK");
        when(procResult.getMensajeTecnicoResultado()).thenReturn("OK");
        final ProcedureRow row = new ProcedureRow(procResult, List.of(15));

        when(procedureExecutor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(1)))
                .thenReturn(row);

        final int eliminadas = repository.depurar(30, 100);
        assertEquals(15, eliminadas);
    }

    @Test
    @DisplayName("BE-J04: marcarEntregada invoca execute correctamente")
    void BE_J04_marcar_entregada() {
        final UUID id = UUID.randomUUID();
        final ProcedureResult procResult = mock(ProcedureResult.class);
        when(procResult.getEstadoResultado()).thenReturn(true);
        when(procedureExecutor.execute(anyString(), anyString(), anyMap(), any())).thenReturn(procResult);

        assertDoesNotThrow(() -> repository.marcarEntregada(id, "instancia-1"));
        verify(procedureExecutor).execute(eq("marcar_notificacion_entregada"), anyString(), anyMap(), any());
    }

    @Test
    @DisplayName("BE-J05: registrarFallo invoca execute con parametros")
    void BE_J05_registrar_fallo() {
        final UUID id = UUID.randomUUID();
        final ProcedureResult procResult = mock(ProcedureResult.class);
        when(procResult.getEstadoResultado()).thenReturn(true);
        when(procedureExecutor.execute(anyString(), anyString(), anyMap(), any())).thenReturn(procResult);

        assertDoesNotThrow(() -> repository.registrarFallo(
                id, "instancia-1", "ERR_CODE", "Detalle error", true, 5, Duration.ofSeconds(30), Duration.ofMinutes(15)
        ));
        verify(procedureExecutor).execute(eq("registrar_fallo_notificacion"), anyString(), anyMap(), any());
    }

    @Test
    @DisplayName("BE-J06: reservar maneja tipos Timestamp y OffsetDateTime")
    void BE_J06_reservar_tipos_fecha() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        final UUID id = UUID.randomUUID();
        final UUID corrId = UUID.randomUUID();
        final java.sql.Timestamp ts = java.sql.Timestamp.from(Instant.now());
        final Object[] fila = {
                id.toString(), "TIPO_B", "Asunto", "Mensaje", "[]", "{}",
                Integer.valueOf(2), ts, corrId, null, null
        };
        when(query.getResultList()).thenReturn(List.of((Object) fila));

        final List<NotificacionReservada> res = repository.reservar(5, Duration.ofSeconds(30), "instancia-2");
        assertEquals(1, res.size());
        assertEquals(id, res.get(0).id());
        assertEquals(ts.toInstant(), res.get(0).creadoEn());
    }

    @Test
    @DisplayName("BE-J07: registrarFallo maneja parametros nulos con defaults")
    void BE_J07_registrar_fallo_defaults() {
        final UUID id = UUID.randomUUID();
        final ProcedureResult procResult = mock(ProcedureResult.class);
        when(procResult.getEstadoResultado()).thenReturn(true);
        when(procedureExecutor.execute(anyString(), anyString(), anyMap(), any())).thenReturn(procResult);

        assertDoesNotThrow(() -> repository.registrarFallo(
                id, "instancia-1", null, null, false, 5, null, null
        ));
    }

    @Test
    @DisplayName("BE-J08: reservar maneja tipos UUID nativo, Instant, OffsetDateTime y nulos")
    void BE_J08_reservar_cobertura_tipos() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        final UUID id = UUID.randomUUID();
        final Instant now = Instant.now();
        final java.time.OffsetDateTime odt = java.time.OffsetDateTime.now();
        final java.time.ZonedDateTime zdt = java.time.ZonedDateTime.now();

        final UUID corrId = UUID.randomUUID();
        final Object[] fila1 = {
                id, "TIPO_1", "A1", "M1", "[]", "{}", "1", now, corrId, "tr1", "sp1"
        };
        final Object[] fila2 = {
                null, "TIPO_2", "A2", "M2", "[]", "{}", null, odt, null, null, null
        };
        final Object[] fila3 = {
                id.toString(), "TIPO_3", "A3", "M3", "[]", "{}", 3, zdt, corrId.toString(), null, null
        };
        final Object[] fila4 = {
                id, "TIPO_4", "A4", "M4", "[]", "{}", 4, now.toString(), corrId, null, null
        };
        final Object[] fila5 = {
                id, "TIPO_5", "A5", "M5", "[]", "{}", 5, null, null, null, null
        };

        when(query.getResultList()).thenReturn(List.of(
                (Object) fila1, (Object) fila2, (Object) fila3, (Object) fila4, (Object) fila5
        ));

        final List<NotificacionReservada> res = repository.reservar(10, Duration.ofSeconds(30), "instancia");
        assertEquals(5, res.size());
        assertEquals(id, res.get(0).id());
        assertNull(res.get(1).id());
        assertEquals(now, res.get(0).creadoEn());
        assertEquals(odt.toInstant(), res.get(1).creadoEn());
        assertEquals(zdt.toInstant(), res.get(2).creadoEn());
        assertEquals(now, res.get(3).creadoEn());
        assertNull(res.get(4).creadoEn());
    }

    @Test
    @DisplayName("BE-J09: reservar falla si orden de columnas esta desplazado con maxIntentos en indice 7")
    void BE_J09_reservar_falla_si_orden_columnas_desplazado() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        final Object[] filaDesplazada = {
                UUID.randomUUID(), "TIPO_A", "Asunto", "Mensaje", "[]", "{}", 1, 5, Instant.now(), "trace", "span"
        };
        when(query.getResultList()).thenReturn(List.of((Object) filaDesplazada));

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> repository.reservar(10, Duration.ofSeconds(60), "instancia-1")
        );
    }

    @Test
    @DisplayName("BE-J10: reservar parsea id y idCorrelacion recibidos como String")
    void BE_J10_reservar_parsea_id_string() {
        final Query query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);

        final UUID id = UUID.randomUUID();
        final UUID corrId = UUID.randomUUID();
        final Instant now = Instant.now();
        final Object[] fila = new Object[]{
                id.toString(), "TIPO_STR", "Asunto", "Mensaje", "[]", "{}", 1, now, corrId.toString(), "tr", "sp"
        };
        when(query.getResultList()).thenReturn(List.of((Object) fila));

        final List<NotificacionReservada> res = repository.reservar(1, Duration.ofSeconds(30), "instancia");
        assertEquals(1, res.size());
        assertEquals(id, res.get(0).id());
        assertEquals(corrId, res.get(0).idCorrelacion());
    }
}
