package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoEvaluacion;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoResolutores;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (HU026C): motor de alertas, grupos pendientes, alertas del estudiante y resolutores sobre SP. */
class AlertaInasistenciaJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private AlertaInasistenciaJpaRepository repo;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new AlertaInasistenciaJpaRepository(manager, new JpaProcedureExecutor(manager));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] canonicaConExtras(final Object... extras) {
        final Object[] fila = new Object[4 + extras.length];
        fila[0] = CORRELACION.toString();
        fila[1] = "mensaje";
        fila[2] = "DBCODE=OK|ok";
        fila[3] = true;
        System.arraycopy(extras, 0, fila, 4, extras.length);
        return fila;
    }

    private static Object[] filaAlerta(final Object total) {
        final Object[] c = new Object[31];
        c[0] = UUID.randomUUID().toString();
        c[4] = GRUPO.toString();
        c[5] = 2;
        c[6] = "G02";
        c[7] = UUID.randomUUID().toString();
        c[8] = "Bases";
        c[10] = "A";
        c[12] = "ADVERTENCIA";
        c[13] = "Riesgo";
        c[15] = "ALR_010";
        c[17] = "VIGENTE";
        c[19] = new BigDecimal("16.00");
        c[20] = new BigDecimal("15.00");
        c[21] = 4;
        c[22] = 25;
        c[23] = LocalDateTime.of(2026, 11, 7, 10, 0);
        c[26] = LocalDateTime.of(2026, 11, 8, 10, 0);
        c[27] = 1;
        c[28] = new BigDecimal("16.50");
        c[29] = new BigDecimal("20.00");
        c[30] = total;
        return c;
    }

    @Test
    void evaluar_ejecuta_el_sp_omitiendo_el_estudiante_grupo_nulo_y_mapea_contadores_y_novedades() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(2, 1, 0, 1, "[]")));

        final ResultadoEvaluacion resultado = repo.evaluar(GRUPO, null);

        assertEquals(2, resultado.alertasCreadas());
        assertEquals(1, resultado.alertasSuperadas());
        assertEquals(0, resultado.alertasReactivadas());
        assertEquals(1, resultado.revisionesCreadas());
        assertTrue(resultado.novedades().isEmpty());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().startsWith("EXEC dbo.usp_evaluar_alertas_inasistencia_grupo"));
        assertFalse(sql.getValue().contains("@idEstudianteGrupo"));
        verify(query).setParameter("idGrupo", GRUPO);
    }

    @Test
    void evaluar_con_json_de_novedades_invalido_es_fallo_tecnico() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(0, 0, 0, 0, "{no es json")));
        assertThrows(DatabaseOperationException.class, () -> repo.evaluar(GRUPO, UUID.randomUUID()));
    }

    @Test
    void consultar_grupos_pendientes_devuelve_los_ids_y_envia_lote_y_periodo_solo_si_existen() {
        final UUID otro = UUID.randomUUID();
        when(query.getResultList()).thenReturn(filas(new Object[]{GRUPO.toString()}, new Object[]{otro}));

        final List<UUID> grupos = repo.consultarGruposPendientes(50, null);

        assertEquals(List.of(GRUPO, otro), grupos);
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().contains("usp_consultar_grupos_pendientes_evaluacion_alertas"));
        assertTrue(sql.getValue().contains("@tamanoLote"));
        assertFalse(sql.getValue().contains("@idPeriodoAcademico"));
        verify(query).setParameter("tamanoLote", 50);
    }

    @Test
    void consultar_grupos_pendientes_traduce_rechazos_y_encapsula_fallos_tecnicos() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultarGruposPendientes(null, UUID.randomUUID()));

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultarGruposPendientes(null, null));
    }

    @Test
    void consultar_alertas_estudiante_mapea_la_pagina_y_los_filtros() {
        when(query.getResultList()).thenReturn(filas(filaAlerta(9L), filaAlerta(4)));

        final PaginaAlertas pagina = repo.consultarAlertasEstudiante(USUARIO, 2, 5, true, false, GRUPO);

        assertEquals(9, pagina.total());
        assertEquals(2, pagina.filas().size());
        final AlertaFila fila = pagina.filas().getFirst();
        assertEquals("ADVERTENCIA", fila.codigoTipo());
        assertEquals("ALR_010", fila.codigoMensajeUsuario());
        assertEquals(new BigDecimal("16.00"), fila.porcentajeCruce());
        assertEquals(new BigDecimal("16.50"), fila.porcentajeActual());
        assertEquals(Instant.parse("2026-11-07T10:00:00Z"), fila.fechaCruceUtc());
        assertEquals(Instant.parse("2026-11-08T10:00:00Z"), fila.fechaLecturaUtc());
        assertTrue(fila.leida());
        assertEquals(GRUPO, fila.grupoId());
        assertEquals(2, fila.codigoGrupo());
        assertEquals(null, fila.mensaje());
        verify(query).setParameter("soloNoLeidas", 1);
        verify(query).setParameter("soloVigentes", 0);
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("numeroPagina", 2);
    }

    @Test
    void consultar_alertas_estudiante_traduce_rechazos_y_encapsula_fallos_tecnicos() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultarAlertasEstudiante(USUARIO, 1, 20, false, false, null));

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultarAlertasEstudiante(USUARIO, 1, 20, false, false, null));
    }

    @Test
    void actualizar_resolutores_mapea_contadores_y_novedades() {
        final String json = "[{\"idRevision\":\"" + UUID.randomUUID() + "\",\"idGrupo\":\"" + GRUPO + "\",\"perfilResolutor\":\"DECANO\","
                + "\"evento\":\"ESCALADA\",\"resolutores\":[{\"idUsuario\":\"" + UUID.randomUUID() + "\",\"nombreCompleto\":\"Decano Uno\","
                + "\"codigoPerfil\":\"DECANO\",\"esEscalamiento\":true}]}]";
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(3, 2, 1, json)));

        final ResultadoResolutores resultado = repo.actualizarResolutores(100);

        assertEquals(3, resultado.reasignadas());
        assertEquals(2, resultado.escaladas());
        assertEquals(1, resultado.sinResolutor());
        assertEquals(1, resultado.novedades().size());
        assertEquals("ESCALADA", resultado.novedades().getFirst().evento());
        assertEquals(1, resultado.novedades().getFirst().resolutores().size());
        verify(query).setParameter("tamanoLote", 100);
    }

    @Test
    void actualizar_resolutores_sin_lote_omite_el_parametro_y_los_fallos_son_tecnicos() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(0, 0, 0, null)));
        assertTrue(repo.actualizarResolutores(null).novedades().isEmpty());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertFalse(sql.getValue().contains("@tamanoLote"));
        verify(query, never()).setParameter(eq("tamanoLote"), any());

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.actualizarResolutores(null));
        assertThrows(DatabaseOperationException.class, () -> repo.evaluar(GRUPO, null));
    }

    @Test
    void los_argumentos_obligatorios_del_constructor_se_validan() {
        assertThrows(NullPointerException.class, () -> new AlertaInasistenciaJpaRepository(null, new JpaProcedureExecutor(manager)));
        assertThrows(NullPointerException.class, () -> new AlertaInasistenciaJpaRepository(manager, null));
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
