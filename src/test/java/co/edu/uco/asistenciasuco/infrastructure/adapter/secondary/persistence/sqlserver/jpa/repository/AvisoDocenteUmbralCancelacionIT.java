package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AvisoAlertasInasistenciaService;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * DEC-065 (1), HU026C: al cruzar el umbral de perdida el docente titular es avisado de inmediato, de punta a punta contra SQL Server real
 * (motor {@code usp_evaluar_alertas_inasistencia_grupo}, servicio de aviso, outbox de notificaciones y publicador realtime). Toma una matricula
 * activa que ya supera el umbral, retira sus alertas y su revision para que la evaluacion vuelva a cruzar, y comprueba la fila
 * {@code REVISION_CANCELACION_PENDIENTE} (destinatario: el docente titular) en {@code NotificacionSaliente} y el evento
 * {@code REVISION_CANCELACION_CREADA}. Todo ocurre en una transaccion que se revierte: no queda ninguna fila ni sale ningun correo (el
 * despachador solo ve filas confirmadas).
 */
@Tag("integration")
@SpringBootTest(properties = "app.adapters.notification.provider=outbox")
@MockitoBean(types = JwtDecoder.class)
@Transactional
class AvisoDocenteUmbralCancelacionIT {

    @Autowired private EvaluadorAlertasInasistencia evaluador;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private RealtimePublisherPort realtime;

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
    }

    @AfterEach
    void limpiar() {
        CorrelationIdContext.clear();
    }

    @Test
    void al_cruzar_el_umbral_se_encola_el_aviso_al_docente_titular_y_se_publica_el_evento_de_revision_creada() {
        assertTrue(evaluador instanceof AvisoAlertasInasistenciaService, "El evaluador cableado debe ser el servicio de avisos.");
        final List<Map<String, Object>> candidatas = jdbc.queryForList("""
                SELECT TOP 1 r.idEstudianteGrupo AS matricula, r.idGrupo AS grupo, d.usuario AS docenteUsuario
                FROM dbo.uv_resumen_asistencia_estudiante_grupo r
                INNER JOIN dbo.Grupo g ON g.id = r.idGrupo
                INNER JOIN dbo.Docente d ON d.id = g.docente
                INNER JOIN dbo.Usuario u ON u.id = d.usuario AND u.estado = 1
                WHERE r.codigoEstadoEstudiante = N'A' AND r.porcentajeInasistencia >= r.umbralPerdida AND r.horasDictadas > 0
                ORDER BY r.porcentajeInasistencia DESC, r.idEstudianteGrupo""");
        assertFalse(candidatas.isEmpty(), "El laboratorio debe tener al menos una matricula activa por encima del umbral de perdida.");
        final String matricula = String.valueOf(candidatas.get(0).get("matricula"));
        final UUID grupo = UUID.fromString(String.valueOf(candidatas.get(0).get("grupo")));
        final String docenteUsuario = String.valueOf(candidatas.get(0).get("docenteUsuario"));

        // Estado previo a cruzar: sin revision ni alertas de la matricula (se revierte al terminar el test).
        jdbc.update("DELETE FROM dbo.RevisionCancelacionHistorial WHERE revision IN (SELECT id FROM dbo.RevisionCancelacion WHERE estudianteGrupo = ?)", matricula);
        jdbc.update("DELETE FROM dbo.RevisionCancelacion WHERE estudianteGrupo = ?", matricula);
        jdbc.update("DELETE FROM dbo.AlertaInasistencia WHERE estudianteGrupo = ?", matricula);

        evaluador.evaluarYAvisar(grupo, UUID.fromString(matricula));

        final List<Map<String, Object>> revisiones = jdbc.queryForList(
                "SELECT id FROM dbo.RevisionCancelacion WHERE estudianteGrupo = ?", matricula);
        assertEquals(1, revisiones.size(), "Al cruzar el umbral la DB crea una sola revision pendiente del docente.");
        final String revision = String.valueOf(revisiones.get(0).get("id")).toLowerCase();

        final List<Map<String, Object>> avisos = jdbc.queryForList(
                "SELECT tipo, destinatarios, datos, asunto FROM dbo.NotificacionSaliente WHERE tipo = ? AND datos LIKE ?",
                Notificacion.TIPO_REVISION_CANCELACION_PENDIENTE, "%" + revision + "%");
        assertEquals(1, avisos.size(), "Debe haber exactamente un aviso REVISION_CANCELACION_PENDIENTE de esa revision en el outbox.");
        final String destinatarios = String.valueOf(avisos.get(0).get("destinatarios")).toLowerCase();
        assertTrue(destinatarios.contains(docenteUsuario.toLowerCase()), "El destinatario del aviso es el docente titular del grupo.");
        assertTrue(destinatarios.contains("docente"), "El destinatario viaja con el rol DOCENTE.");
        assertNotNull(avisos.get(0).get("asunto"));

        final ArgumentCaptor<RealtimeEvent> eventos = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, atLeastOnce()).publish(eventos.capture());
        final RealtimeEvent creada = eventos.getAllValues().stream()
                .filter(e -> AvisoAlertasInasistenciaService.EVENTO_REVISION_CREADA.equals(e.type()))
                .findFirst().orElseThrow(() -> new AssertionError("No se publico REVISION_CANCELACION_CREADA."));
        assertEquals(java.util.Set.of(UUID.fromString(docenteUsuario)), creada.destinatariosUsuario(),
                "El evento REVISION_CANCELACION_CREADA es solo para el docente titular.");
        assertEquals(revision, String.valueOf(creada.payload().get("revision")).toLowerCase());
        assertEquals("PENDIENTE_DOCENTE", creada.payload().get("estado"));
        verify(realtime, atLeastOnce()).publish(any());
    }
}
