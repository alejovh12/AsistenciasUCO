package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudAprobada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudRechazada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 / DEC-064: adaptador JPA de los SP de las sesiones manuales (destinatarios, cancelacion del coordinador y motivos). */
class SesionManualJpaRepositoryTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final JpaProcedureExecutor executor = mock(JpaProcedureExecutor.class);
    private final Query query = mock(Query.class);
    private final SesionManualJpaRepository repository = new SesionManualJpaRepository(entityManager, executor);
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

    @Test
    @SuppressWarnings("unchecked")
    void cancelar_envia_los_parametros_del_sp_incluida_la_observacion_nula_y_lee_sus_cuatro_columnas() {
        final LocalDateTime cancelada = LocalDateTime.of(2026, 11, 9, 14, 0);
        when(executor.executeWithExtras(eq("cancelarSesionCoordinador"), anyString(), anyMap(), eq(correlacion), eq(4)))
                .thenReturn(new ProcedureRow(null, List.of(SESION.toString(), GRUPO, cancelada, 38)));

        assertEquals(new SesionCancelada(SESION, GRUPO, cancelada, 38), repository.cancelarPorCoordinador(SESION, "DUPLICADA", null, USUARIO));

        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq("cancelarSesionCoordinador"), eq(SesionManualJpaRepository.SQL_CANCELAR_COORDINADOR),
                parametros.capture(), eq(correlacion), eq(4));
        final Map<String, Object> esperados = new HashMap<>();
        esperados.put("idSesion", SESION);
        esperados.put("codigoMotivo", "DUPLICADA");
        esperados.put("observacion", null);
        esperados.put("idCorrelacion", correlacion);
        esperados.put("idUsuarioEjecutor", USUARIO);
        assertEquals(esperados, parametros.getValue());
    }

    @Test
    void cancelar_tolera_conteo_nulo_y_fecha_nula() {
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(null, Arrays.asList(SESION, GRUPO, null, null)));

        final SesionCancelada cancelada = repository.cancelarPorCoordinador(SESION, "OTRO", "obs", USUARIO);

        assertEquals(0, cancelada.totalEstudiantesActivos());
        assertNull(cancelada.fechaCancelacionUtc());
    }

    @Test
    void destinatarios_mapea_las_nueve_columnas_del_sp() {
        final UUID usuario = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 13, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{"ESTUDIANTE", usuario.toString(), "Ana Perez", "ana@uco.edu.co", "Calculo", "G01 de Calculo", "Maria Docente", inicio, inicio.plusHours(2)},
                new Object[]{"COORDINADOR", usuario, null, null, null, null, null, null, null}));

        final List<DestinatarioSesionManual> destinatarios = repository.consultarDestinatarios(SESION, USUARIO);

        assertEquals(new DestinatarioSesionManual("ESTUDIANTE", usuario, "Ana Perez", "ana@uco.edu.co", "Calculo", "G01 de Calculo",
                "Maria Docente", inicio, inicio.plusHours(2)), destinatarios.getFirst());
        assertNull(destinatarios.get(1).nombreCompleto());
        verify(entityManager).createNativeQuery(SesionManualJpaRepository.SQL_DESTINATARIOS);
        verify(query).setParameter("idSesion", SESION);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void destinatarios_traduce_el_dbcode_de_la_consulta() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"));
        assertThrows(ForbiddenException.class, () -> repository.consultarDestinatarios(SESION, USUARIO));
    }

    @Test
    void motivos_mapea_codigo_y_nombre() {
        when(query.getResultList()).thenReturn(List.<Object>of(new Object[]{"DUPLICADA", "Sesión duplicada"}, new Object[]{"OTRO", null}));

        assertEquals(List.of(new MotivoCancelacion("DUPLICADA", "Sesión duplicada"), new MotivoCancelacion("OTRO", null)),
                repository.consultarMotivosCancelacion(USUARIO));
        verify(entityManager).createNativeQuery(SesionManualJpaRepository.SQL_MOTIVOS);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
        verify(query).setParameter("idCorrelacion", correlacion);
    }

    @Test
    void motivos_traduce_el_dbcode_de_la_consulta() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"));
        assertThrows(ForbiddenException.class, () -> repository.consultarMotivosCancelacion(USUARIO));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aprobar_envia_los_parametros_del_sp_y_devuelve_el_mensaje_del_sp() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 13, 0);
        final ProcedureResult resultadoSp = resultado("Aprobada.");
        when(executor.executeWithExtras(eq("aprobarSolicitudSesion"), anyString(), anyMap(), eq(correlacion), eq(4)))
                .thenReturn(new ProcedureRow(resultadoSp, List.of(SESION.toString(), GRUPO, inicio, 30)));

        assertEquals(new SolicitudAprobada(SESION, GRUPO, inicio, 30, "Aprobada."), repository.aprobarSolicitud(SESION, null, USUARIO));

        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq("aprobarSolicitudSesion"), eq(SesionManualJpaRepository.SQL_APROBAR_SOLICITUD),
                parametros.capture(), eq(correlacion), eq(4));
        final Map<String, Object> esperados = new HashMap<>();
        esperados.put("idSesion", SESION);
        esperados.put("observacion", null);
        esperados.put("idCorrelacion", correlacion);
        esperados.put("idUsuarioEjecutor", USUARIO);
        assertEquals(esperados, parametros.getValue());
    }

    @Test
    void aprobar_tolera_conteo_nulo() {
        final ProcedureResult resultadoSp = resultado("ok");
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(resultadoSp, Arrays.asList(SESION, GRUPO, null, null)));

        final SolicitudAprobada aprobada = repository.aprobarSolicitud(SESION, "obs", USUARIO);

        assertEquals(0, aprobada.totalEstudiantesActivos());
        assertNull(aprobada.fechaHoraInicio());
    }

    @Test
    @SuppressWarnings("unchecked")
    void rechazar_envia_motivo_y_observacion_y_devuelve_el_mensaje_del_sp() {
        final LocalDateTime cancelada = LocalDateTime.of(2026, 11, 9, 14, 0);
        final ProcedureResult resultadoSp = resultado("Rechazada.");
        when(executor.executeWithExtras(eq("rechazarSolicitudSesion"), anyString(), anyMap(), eq(correlacion), eq(4)))
                .thenReturn(new ProcedureRow(resultadoSp, List.of(SESION, GRUPO, cancelada, 12)));

        assertEquals(new SolicitudRechazada(SESION, GRUPO, cancelada, 12, "Rechazada."),
                repository.rechazarSolicitud(SESION, "DUPLICADA", "Ya existe", USUARIO));

        final ArgumentCaptor<Map<String, Object>> parametros = ArgumentCaptor.forClass(Map.class);
        verify(executor).executeWithExtras(eq("rechazarSolicitudSesion"), eq(SesionManualJpaRepository.SQL_RECHAZAR_SOLICITUD),
                parametros.capture(), eq(correlacion), eq(4));
        assertEquals("DUPLICADA", parametros.getValue().get("codigoMotivo"));
        assertEquals("Ya existe", parametros.getValue().get("observacion"));
        assertEquals(USUARIO, parametros.getValue().get("idUsuarioEjecutor"));
    }

    @Test
    void rechazar_tolera_conteo_nulo() {
        final ProcedureResult resultadoSp = resultado("ok");
        when(executor.executeWithExtras(anyString(), anyString(), anyMap(), any(), eq(4)))
                .thenReturn(new ProcedureRow(resultadoSp, Arrays.asList(SESION, GRUPO, null, null)));

        assertEquals(0, repository.rechazarSolicitud(SESION, "OTRO", null, USUARIO).totalEstudiantesActivos());
    }

    @Test
    void destinatarios_de_la_solicitud_usan_el_sp_del_coordinador_y_mapean_las_nueve_columnas() {
        final UUID usuario = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 13, 0);
        when(query.getResultList()).thenReturn(java.util.Collections.singletonList(
                (Object) new Object[]{"DOCENTE", usuario, "Maria Docente", "maria@uco.edu.co", "Calculo", "G01 de Calculo", "Maria Docente", inicio, inicio.plusHours(2)}));

        final List<DestinatarioSesionManual> destinatarios = repository.consultarDestinatariosSolicitud(SESION, USUARIO);

        assertEquals(new DestinatarioSesionManual("DOCENTE", usuario, "Maria Docente", "maria@uco.edu.co", "Calculo", "G01 de Calculo",
                "Maria Docente", inicio, inicio.plusHours(2)), destinatarios.getFirst());
        verify(entityManager).createNativeQuery(SesionManualJpaRepository.SQL_DESTINATARIOS_SOLICITUD);
        verify(query).setParameter("idUsuarioEjecutor", USUARIO);
    }

    @Test
    void destinatarios_de_la_solicitud_traducen_el_dbcode_de_ambito() {
        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_002|sin ambito"));
        assertThrows(ForbiddenException.class, () -> repository.consultarDestinatariosSolicitud(SESION, USUARIO));
    }

    @Test
    void solicitudes_mapea_las_veinte_columnas_y_el_total() {
        final UUID docente = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 10, 13, 0);
        final LocalDateTime solicitada = LocalDateTime.of(2026, 11, 9, 14, 0);
        when(query.getResultList()).thenReturn(List.<Object>of(
                new Object[]{SESION.toString(), "Repaso", "S-9", GRUPO, "G01", "Calculo", programa, "Ingenieria", docente, "Maria Docente",
                        inicio, inicio.plusHours(2), "PENDIENTE", "Pendiente", solicitada, null, null, "maria", true, false, 7},
                new Object[]{SESION, null, null, GRUPO, null, null, programa, null, docente, null, null, null, null, null, null, null, null,
                        null, true, false, 3}));

        final PaginaSolicitudesSesion pagina = repository.consultarSolicitudes(USUARIO, true, programa, GRUPO, 1, 20);

        assertEquals(7, pagina.total());
        assertEquals(new SolicitudSesion(SESION, "Repaso", "S-9", GRUPO, "G01", "Calculo", programa, "Ingenieria", docente, "Maria Docente",
                inicio, inicio.plusHours(2), "PENDIENTE", "Pendiente", solicitada, null, null), pagina.elementos().getFirst());
        assertNull(pagina.elementos().get(1).nombreSesion());
        verify(entityManager).createNativeQuery(
                "EXEC dbo.usp_consultar_solicitudes_sesion_coordinador_paginado @idUsuario = :idUsuario, @soloPendientes = :soloPendientes, "
                        + "@idPrograma = :idPrograma, @idGrupo = :idGrupo, @numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina, "
                        + "@idCorrelacion = :idCorrelacion");
        verify(query).setParameter("soloPendientes", 1);
        verify(query).setParameter("idUsuario", USUARIO);
    }

    @Test
    void solicitudes_sin_filtros_omite_los_parametros_nulos_y_traduce_el_dbcode() {
        when(query.getResultList()).thenReturn(List.of());

        final PaginaSolicitudesSesion vacia = repository.consultarSolicitudes(USUARIO, false, null, null, 2, 5);

        assertEquals(0, vacia.total());
        verify(entityManager).createNativeQuery(
                "EXEC dbo.usp_consultar_solicitudes_sesion_coordinador_paginado @idUsuario = :idUsuario, @soloPendientes = :soloPendientes, "
                        + "@numeroPagina = :numeroPagina, @tamanoPagina = :tamanoPagina, @idCorrelacion = :idCorrelacion");
        verify(query).setParameter("soloPendientes", 0);

        when(query.getResultList()).thenThrow(new PersistenceException("DBCODE=SEC_001|sin perfil"));
        assertThrows(ForbiddenException.class, () -> repository.consultarSolicitudes(USUARIO, true, null, null, 1, 20));
    }

    private static ProcedureResult resultado(final String mensajeUsuario) {
        final ProcedureResult resultado = mock(ProcedureResult.class);
        when(resultado.getMensajeUsuarioResultado()).thenReturn(mensajeUsuario);
        return resultado;
    }

    @Test
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new SesionManualJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new SesionManualJpaRepository(entityManager, null));
    }
}
