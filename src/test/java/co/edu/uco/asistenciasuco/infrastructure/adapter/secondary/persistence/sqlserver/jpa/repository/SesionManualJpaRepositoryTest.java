package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
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
    void el_constructor_exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new SesionManualJpaRepository(null, executor));
        assertThrows(NullPointerException.class, () -> new SesionManualJpaRepository(entityManager, null));
    }
}
