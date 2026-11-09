package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.DestinatarioRevision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.RevisionFila;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/** CULM-001 C.1 (HU026C): decision, resolucion, bandejas y destinatarios de revisiones de cancelacion sobre SP. */
class RevisionCancelacionJpaRepositoryTest {

    private static final UUID CORRELACION = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID REVISION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();

    private final EntityManager manager = mock(EntityManager.class);
    private final Query query = mock(Query.class);
    private RevisionCancelacionJpaRepository repo;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(CORRELACION);
        when(manager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        repo = new RevisionCancelacionJpaRepository(manager, new JpaProcedureExecutor(manager));
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    private static Object[] canonicaConExtras(final Object... extras) {
        final Object[] fila = new Object[4 + extras.length];
        fila[0] = CORRELACION.toString();
        fila[1] = "mensaje para el usuario";
        fila[2] = "DBCODE=OK|ok";
        fila[3] = true;
        System.arraycopy(extras, 0, fila, 4, extras.length);
        return fila;
    }

    private static Object[] filaBandeja(final Object total) {
        final Object[] c = new Object[54];
        c[0] = REVISION.toString();
        c[2] = UUID.randomUUID().toString();
        c[4] = "Estudiante Uno";
        c[5] = GRUPO.toString();
        c[6] = 3;
        c[7] = "G03";
        c[8] = UUID.randomUUID().toString();
        c[9] = "Bases de Datos";
        c[11] = UUID.randomUUID().toString();
        c[13] = "Docente Uno";
        c[14] = "ACTIVO";
        c[16] = "PROPUESTA";
        c[17] = "Cancelacion propuesta";
        c[19] = 1;
        c[20] = 0;
        c[24] = new BigDecimal("21.43");
        c[27] = LocalDateTime.of(2026, 11, 7, 10, 0);
        c[28] = "PROPONER";
        c[29] = "motivo docente";
        c[32] = LocalDateTime.of(2026, 11, 7, 11, 0);
        c[33] = "DECANO";
        c[34] = LocalDateTime.of(2026, 11, 8, 8, 0);
        c[35] = "APROBAR";
        c[36] = "motivo resolucion";
        c[38] = "Decano Uno";
        c[39] = "DECANO";
        c[43] = LocalDateTime.of(2026, 11, 8, 9, 0);
        c[45] = PROGRAMA.toString();
        c[46] = "Ingenieria";
        c[47] = new BigDecimal("22.50");
        c[48] = 9;
        c[49] = "40";
        c[50] = 20;
        c[51] = true;
        c[52] = "APROBAR, RECHAZAR , ";
        c[53] = total;
        return c;
    }

    @Test
    void decidir_ejecuta_el_sp_sin_parametros_nulos_y_mapea_las_12_columnas_extra() {
        final UUID grupo = UUID.randomUUID();
        final UUID estudianteGrupo = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(
                REVISION.toString(), "PENDIENTE_DOCENTE", "PROPUESTA", "PROPONER", 0, grupo.toString(), estudianteGrupo.toString(),
                estudiante.toString(), 22, "PROPUESTA_CANCELACION", "COORDINADOR", 1)));

        final ResultadoDecision resultado = repo.decidir(REVISION, "PROPONER", null, USUARIO);

        assertEquals("mensaje para el usuario", resultado.mensajeUsuario());
        assertEquals(REVISION, resultado.idRevision());
        assertEquals("PROPUESTA", resultado.estadoNuevo());
        assertFalse(resultado.sinCambios());
        assertEquals(grupo, resultado.idGrupo());
        assertEquals(new BigDecimal("22"), resultado.porcentajeActual());
        assertEquals("COORDINADOR", resultado.perfilResolutor());
        assertTrue(resultado.escalamiento());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().startsWith("EXEC dbo.usp_decidir_revision_cancelacion"));
        assertFalse(sql.getValue().contains("@motivo"));
        verify(query).setParameter("idRevision", REVISION);
        verify(query).setParameter("codigoDecision", "PROPONER");
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
    }

    @Test
    void resolver_ejecuta_el_sp_y_mapea_las_13_columnas_extra() {
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(canonicaConExtras(
                REVISION.toString(), "PROPUESTA", "APROBADA", "APROBAR", 0, true, GRUPO.toString(), UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), PROGRAMA.toString(), "COORDINADOR", new BigDecimal("21.5"), "CANCELACION_APROBADA")));

        final ResultadoResolucion resultado = repo.resolver(REVISION, "APROBAR", "motivo suficiente", USUARIO);

        assertEquals("APROBADA", resultado.estadoNuevo());
        assertTrue(resultado.cambioMatricula());
        assertFalse(resultado.sinCambios());
        assertEquals(PROGRAMA, resultado.idPrograma());
        assertEquals("COORDINADOR", resultado.perfilResolucion());
        assertEquals(new BigDecimal("21.5"), resultado.porcentajeActual());
        assertEquals("CANCELACION_APROBADA", resultado.tipoPlantillaNotificacion());
        verify(query).setParameter("motivo", "motivo suficiente");
    }

    @Test
    void consultar_coordinador_mapea_la_bandeja_y_envia_solo_los_filtros_presentes() {
        when(query.getResultList()).thenReturn(filas(filaBandeja(5L), filaBandeja(3)));

        final PaginaRevisiones pagina = repo.consultarCoordinador(USUARIO, 2, 10, true, "  PROPUESTA ", PROGRAMA, GRUPO);

        assertEquals(5, pagina.total());
        assertEquals(2, pagina.filas().size());
        final RevisionFila fila = pagina.filas().getFirst();
        assertEquals(REVISION, fila.id());
        assertEquals("PROPUESTA", fila.estado());
        assertTrue(fila.requiereAccionDocente());
        assertFalse(fila.requiereAccionResolutor());
        assertTrue(fila.esResolutorActual());
        assertEquals(List.of("APROBAR", "RECHAZAR"), fila.accionesPermitidas());
        assertEquals(3, fila.codigoGrupo());
        assertEquals("Ingenieria", fila.nombrePrograma());
        assertEquals(new BigDecimal("22.50"), fila.porcentajeActual());
        assertEquals(new BigDecimal("9"), fila.horasFalta());
        assertEquals("Decano Uno", fila.nombreResolutor());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().contains("usp_consultar_revisiones_cancelacion_coordinador_paginado"));
        assertTrue(sql.getValue().contains("@idGrupo"));
        verify(query).setParameter("soloPendientes", 1);
        verify(query).setParameter("codigoEstado", "PROPUESTA");
        verify(query).setParameter("idPrograma", PROGRAMA);
        verify(query).setParameter("idGrupo", GRUPO);
        verify(query).setParameter("idCorrelacion", CORRELACION);
    }

    @Test
    void consultar_decano_sin_filtros_omite_estado_programa_y_acciones_vacias() {
        final Object[] sinAcciones = filaBandeja(1L);
        sinAcciones[52] = null;
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(sinAcciones));

        final PaginaRevisiones pagina = repo.consultarDecano(USUARIO, 1, 20, false, "  ", null);

        assertTrue(pagina.filas().getFirst().accionesPermitidas().isEmpty());
        final ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(manager).createNativeQuery(sql.capture());
        assertTrue(sql.getValue().contains("usp_consultar_revisiones_cancelacion_decano_paginado"));
        assertFalse(sql.getValue().contains("@codigoEstado"));
        assertFalse(sql.getValue().contains("@idPrograma"));
        verify(query).setParameter("soloPendientes", 0);
        verify(query, never()).setParameter(eq("codigoEstado"), any());
    }

    @Test
    void consultar_destinatarios_mapea_19_columnas() {
        final Object[] c = {"DOCENTE", "DOC", UUID.randomUUID().toString(), "Doc Uno", "doc@uco.edu.co", PROGRAMA.toString(), 1, GRUPO.toString(), 3, "G03",
                "Bases", "Est Uno", "Doc Uno", "PROPUESTA", new BigDecimal("21"), 20, "motivo docente", null, null};
        when(query.getResultList()).thenReturn(Collections.<Object>singletonList(c));

        final List<DestinatarioRevision> destinatarios = repo.consultarDestinatarios(USUARIO, REVISION);

        assertEquals(1, destinatarios.size());
        final DestinatarioRevision d = destinatarios.getFirst();
        assertEquals("doc@uco.edu.co", d.correo());
        assertTrue(d.esEscalamiento());
        assertEquals(3, d.codigoGrupo());
        assertEquals(new BigDecimal("20"), d.umbralPerdida());
        assertNull(d.nombreResolutor());
        verify(query).setParameter("idRevision", REVISION);
    }

    @Test
    void los_rechazos_de_las_consultas_se_traducen_y_los_tecnicos_no() {
        doThrow(new PersistenceException("x", new RuntimeException("DBCODE=SEC_001|perfil"))).when(query).getResultList();
        assertThrows(ForbiddenException.class, () -> repo.consultarDecano(USUARIO, 1, 20, false, null, null));
        assertThrows(ForbiddenException.class, () -> repo.consultarDestinatarios(USUARIO, REVISION));

        doThrow(new PersistenceException("caida")).when(query).getResultList();
        assertThrows(DatabaseOperationException.class, () -> repo.consultarCoordinador(USUARIO, 1, 20, false, null, null, null));
        assertThrows(DatabaseOperationException.class, () -> repo.decidir(REVISION, "PROPONER", "m", USUARIO));
        assertThrows(DatabaseOperationException.class, () -> repo.resolver(REVISION, "APROBAR", "m", USUARIO));
    }

    @Test
    void los_argumentos_obligatorios_del_constructor_se_validan() {
        assertThrows(NullPointerException.class, () -> new RevisionCancelacionJpaRepository(null, new JpaProcedureExecutor(manager)));
        assertThrows(NullPointerException.class, () -> new RevisionCancelacionJpaRepository(manager, null));
    }

    /** Lista de filas nativas: evita que {@code List.of} expanda el arreglo de columnas como varargs. */
    private static List<Object> filas(final Object[]... filas) {
        return new java.util.ArrayList<>(java.util.Arrays.asList((Object[]) filas));
    }
}
