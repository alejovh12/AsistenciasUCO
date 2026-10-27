package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU053-HU055: EXEC de los SP de solicitudes de ingreso con binding nombrado y correlacion (RG-01..RG-06). */
class SolicitudIngresoGrupoJpaRepositoryTest {

    private static final UUID CORRELATION = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SOLICITUD = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private SolicitudIngresoGrupoJpaRepository repository;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELATION);
        repository = new SolicitudIngresoGrupoJpaRepository(new JpaProcedureExecutor(entityManager), entityManager);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new SolicitudIngresoGrupoJpaRepository(null, entityManager));
        assertThrows(NullPointerException.class,
                () -> new SolicitudIngresoGrupoJpaRepository(new JpaProcedureExecutor(entityManager), null));
    }

    @Test
    void RG01_registrar_y_retirar_ejecutan_sus_sp() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true}));

        repository.registrarSolicitud(SOLICITUD, GRUPO, ACTOR);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_registrar_solicitud_ingreso_grupo"));
        verify(query).setParameter("idSolicitud", SOLICITUD);
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idCorrelacion", CORRELATION);
        verify(query).setParameter("idUsuarioEjecutor", ACTOR);

        repository.retirarSolicitud(SOLICITUD, ACTOR);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_retirar_solicitud_ingreso_grupo"));
    }

    @Test
    void RG02_resolver_devuelve_el_codigo_de_estado_resultante() {
        when(query.getResultList()).thenReturn(Collections.singletonList(new Object[]{CORRELATION, "ok", "", true, "E"}));

        assertEquals("E", repository.resolverSolicitud(SOLICITUD, "ACEPTAR", null, ACTOR));
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_resolver_solicitud_ingreso_grupo"));
        verify(query).setParameter("accion", "ACEPTAR");
        verify(query).setParameter("motivo", null);
    }

    @Test
    void RG03_ejecutor_nulo_no_llega_a_la_db() {
        assertThrows(CrosscuttingException.class, () -> repository.registrarSolicitud(SOLICITUD, GRUPO, null));
        verify(entityManager, never()).createNativeQuery(anyString());
    }

    @Test
    void RG04_bandeja_del_docente_mapea_fechas_utc_y_cupo() {
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{SOLICITUD, UUID.randomUUID(), "Beto Qa", 970005302, "E",
                "en espera", Timestamp.valueOf(LocalDateTime.of(2026, 10, 27, 13, 0)), null, null, 1, 30, 0, 4}));

        final var filas = repository.consultarSolicitudesGrupo(GRUPO, "E", 1, 10, ACTOR);

        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_solicitudes_ingreso_grupo_docente"));
        verify(query).setParameter("codigoEstado", "E");
        verify(query).setParameter("numeroPagina", 1);
        verify(query).setParameter("tamanoPagina", 10);
        final var fila = filas.getFirst();
        assertEquals(Instant.parse("2026-10-27T13:00:00Z"), fila.fechaSolicitudUtc());
        assertNull(fila.fechaResolucionUtc());
        assertEquals(1, fila.posicionEspera());
        assertEquals(0, fila.cuposDisponibles());
        assertEquals(4, fila.totalRegistros());
    }

    @Test
    void RG05_mis_solicitudes_y_destinatarios() {
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{SOLICITUD, GRUPO, 1, "Grupo 1", UUID.randomUUID(),
                "Calculo", "Doc", "R", "rechazada", LocalDateTime.of(2026, 10, 27, 13, 0),
                LocalDateTime.of(2026, 10, 27, 14, 0), "Motivo", 1}));
        final var mias = repository.consultarSolicitudesEstudiante(null, 1, 10, ACTOR);
        assertEquals("Motivo", mias.getFirst().motivoRechazo());
        assertEquals(Instant.parse("2026-10-27T14:00:00Z"), mias.getFirst().fechaResolucionUtc());

        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{"DOCENTE", ACTOR, "Doc", "d@uco.edu.co", GRUPO, 1, "Grupo 1", "Calculo"}));
        final var destinatarios = repository.consultarDestinatarios(SOLICITUD, ACTOR);
        verify(entityManager).createNativeQuery(contains("EXEC dbo.usp_consultar_destinatarios_solicitud_ingreso_grupo"));
        assertEquals("DOCENTE", destinatarios.getFirst().rol());
    }

    @Test
    void RG06_fallo_tecnico_de_una_consulta_se_traduce() {
        when(query.getResultList()).thenThrow(new PersistenceException("timeout"));
        assertThrows(DatabaseOperationException.class, () -> repository.consultarSolicitudesEstudiante(null, 1, 10, ACTOR));
    }
}
