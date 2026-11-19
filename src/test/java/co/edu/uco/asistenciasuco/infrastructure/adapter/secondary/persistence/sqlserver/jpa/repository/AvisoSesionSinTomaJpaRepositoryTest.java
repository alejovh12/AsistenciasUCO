package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.DestinatarioAvisoSinToma;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.SesionSinTomaPendiente;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTOMA-001: adaptador JPA del aviso de sesion sin toma (EXEC nativo, columnas leidas por nombre y comando canonico). */
class AvisoSesionSinTomaJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final AvisoSesionSinTomaJpaRepository repository = new AvisoSesionSinTomaJpaRepository(entityManager, executor);
    private UUID correlacion;

    @BeforeEach
    void preparar() {
        correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        when(entityManager.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void sesiones_pendientes_lee_las_columnas_por_nombre_y_envia_la_correlacion() {
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(Map.of(
                "idSesion", SESION.toString(), "idGrupo", GRUPO.toString(), "fechaHoraFinUtc", Timestamp.valueOf("2026-11-17 15:00:00")))));

        final List<SesionSinTomaPendiente> pendientes = repository.consultarSesionesPendientes();

        assertEquals(List.of(new SesionSinTomaPendiente(SESION, GRUPO, Instant.parse("2026-11-17T15:00:00Z"))), pendientes);
        verify(entityManager).createNativeQuery(AvisoSesionSinTomaJpaRepository.SQL_PENDIENTES, Tuple.class);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void destinatarios_de_la_sesion_mapea_todas_las_columnas() {
        final Map<String, Object> fila = new HashMap<>();
        fila.put("idSesion", SESION.toString());
        fila.put("idGrupo", GRUPO.toString());
        fila.put("idAsignatura", ASIGNATURA.toString());
        fila.put("nombreAsignatura", "Ingeniería de Software");
        fila.put("nombreGrupo", "G01 de Ingeniería de Software");
        fila.put("nombreDocente", "María Gómez");
        fila.put("fechaHoraInicioUtc", Timestamp.valueOf("2026-11-17 13:00:00"));
        fila.put("fechaHoraFinUtc", Timestamp.valueOf("2026-11-17 15:00:00"));
        fila.put("idUsuarioCoordinador", USUARIO.toString());
        fila.put("nombreCoordinador", "Laura Martinez");
        fila.put("correoCoordinador", "laura@uco.test");
        when(query.getResultList()).thenReturn(List.of(TomaAsistenciaJpaRepositoryTest.tupla(fila)));

        final List<DestinatarioAvisoSinToma> destinatarios = repository.consultarDestinatarios(SESION);

        assertEquals(1, destinatarios.size());
        final DestinatarioAvisoSinToma d = destinatarios.getFirst();
        assertEquals(SESION, d.idSesion());
        assertEquals(GRUPO, d.idGrupo());
        assertEquals(ASIGNATURA, d.idAsignatura());
        assertEquals("Ingeniería de Software", d.nombreAsignatura());
        assertEquals("G01 de Ingeniería de Software", d.nombreGrupo());
        assertEquals("María Gómez", d.nombreDocente());
        assertEquals(Instant.parse("2026-11-17T13:00:00Z"), d.fechaHoraInicioUtc());
        assertEquals(Instant.parse("2026-11-17T15:00:00Z"), d.fechaHoraFinUtc());
        assertEquals(USUARIO, d.idUsuarioCoordinador());
        assertEquals("Laura Martinez", d.nombreCoordinador());
        assertEquals("laura@uco.test", d.correoCoordinador());
        verify(entityManager).createNativeQuery(AvisoSesionSinTomaJpaRepository.SQL_DESTINATARIOS, Tuple.class);
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void registrar_aviso_ejecuta_el_comando_canonico_con_la_correlacion() {
        repository.registrarAviso(SESION, 3);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, ?>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq("registrarAvisoSesionSinToma"), eq(AvisoSesionSinTomaJpaRepository.SQL_REGISTRAR), parametros.capture(), eq(correlacion));
        assertEquals(SESION, parametros.getValue().get("idSesion"));
        assertEquals(3, parametros.getValue().get("totalCoordinadoresAvisados"));
        assertEquals(correlacion, parametros.getValue().get("idCorrelacion"));
        assertTrue(AvisoSesionSinTomaJpaRepository.SQL_REGISTRAR.contains("usp_registrar_aviso_sesion_sin_toma"));
    }

    @Test
    void un_fallo_de_persistencia_en_la_consulta_se_traduce_a_error_de_base_de_datos() {
        when(query.getResultList()).thenThrow(new PersistenceException("db"));

        assertThrows(DatabaseOperationException.class, repository::consultarSesionesPendientes);
        assertThrows(DatabaseOperationException.class, () -> repository.consultarDestinatarios(SESION));
    }

    @Test
    void sin_correlacion_activa_no_consulta() {
        CorrelationIdContext.clear();

        assertThrows(RuntimeException.class, repository::consultarSesionesPendientes);
        assertThrows(RuntimeException.class, () -> repository.consultarDestinatarios(SESION));
        assertThrows(RuntimeException.class, () -> repository.registrarAviso(SESION, 1));
        verify(executor, never()).execute(anyString(), anyString(), anyMap(), any());
    }
}
