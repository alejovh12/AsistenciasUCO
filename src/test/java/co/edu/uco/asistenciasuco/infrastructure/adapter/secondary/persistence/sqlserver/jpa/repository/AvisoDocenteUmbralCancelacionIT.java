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
 * (motor {@code usp_evaluar_alertas_inasistencia_grupo}, servicio de aviso, outbox de notificaciones y publicador realtime). Crea una matricula
 * activa que ya supera el umbral (grupo propio con tres faltas SJC en sesiones vencidas), retira sus alertas y su revision para que la evaluacion vuelva a cruzar, y comprueba la fila
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
        // VAL-PRU-04: la matricula por encima del umbral la crea el propio test (dentro de su transaccion, que se revierte) en vez
        // de depender de datos del laboratorio: el esquema desplegado en CI no trae asistencias historicas.
        final String etiqueta = crearMatriculaSobreUmbral();
        final List<Map<String, Object>> candidatas = jdbc.queryForList("""
                SELECT TOP 1 r.idEstudianteGrupo AS matricula, r.idGrupo AS grupo, d.usuario AS docenteUsuario
                FROM dbo.uv_resumen_asistencia_estudiante_grupo r
                INNER JOIN dbo.Grupo g ON g.id = r.idGrupo
                INNER JOIN dbo.Docente d ON d.id = g.docente
                INNER JOIN dbo.Usuario u ON u.id = d.usuario AND u.estado = 1
                WHERE r.codigoEstadoEstudiante = N'A' AND r.porcentajeInasistencia >= r.umbralPerdida AND r.horasDictadas > 0
                  AND g.nombre = ?
                ORDER BY r.porcentajeInasistencia DESC, r.idEstudianteGrupo""", etiqueta);
        assertFalse(candidatas.isEmpty(), "El fixture debe dejar una matricula activa por encima del umbral de perdida.");
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

    /**
     * Grupo propio (periodo vigente, docente y estudiante nuevos, activos) con tres sesiones ya vencidas de procedencia UTC
     * confirmada, cada una con su toma 1 cerrada y la falta SJC del estudiante: 100 % de inasistencia por horas, sobre el
     * umbral de perdida. Todo vive en la transaccion del test y se revierte al terminar. Devuelve el nombre del grupo.
     */
    private String crearMatriculaSobreUmbral() {
        final String etiqueta = "IT-AVISO-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                DECLARE @tag NVARCHAR(40) = ?;
                DECLARE @tipoCC UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion = 'CC');
                DECLARE @idAsig UNIQUEIDENTIFIER = (SELECT TOP 1 a.id FROM dbo.uv_asignatura a WHERE a.estaActivaAsignatura = 1 ORDER BY a.id);
                DECLARE @idInst UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.uv_institucion ORDER BY id);
                DECLARE @estadoA UNIQUEIDENTIFIER = (SELECT id FROM dbo.uv_estado_estudiante_grupo WHERE codigo = 'A');
                DECLARE @sjc UNIQUEIDENTIFIER = (SELECT id FROM dbo.RazonCausa WHERE codigo = 'SJC');
                DECLARE @modo UNIQUEIDENTIFIER = (SELECT TOP 1 m.id FROM dbo.ModoTomaAsistencia m WHERE m.usaCodigoQr = 0);
                DECLARE @cerrada UNIQUEIDENTIFIER = (SELECT TOP 1 e.id FROM dbo.EstadoTomaAsistencia e WHERE e.estaAbierta = 0);
                DECLARE @uD UNIQUEIDENTIFIER = NEWID(), @d UNIQUEIDENTIFIER = NEWID(), @uE UNIQUEIDENTIFIER = NEWID(), @e UNIQUEIDENTIFIER = NEWID();
                DECLARE @per UNIQUEIDENTIFIER = NEWID(), @g UNIQUEIDENTIFIER = NEWID(), @eg UNIQUEIDENTIFIER = NEWID();
                DECLARE @hoy DATE = CAST(SYSUTCDATETIME() AS DATE);
                DECLARE @base INT = 1500000000 + ABS(CHECKSUM(NEWID()) % 400000000);
                INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre,
                                         segundoNombre, correo, correoConfirmado, estado, password)
                VALUES (@uD, @tipoCC, @base, N'Aviso', N'IT', N'Doc', N'IT', CONCAT(@tag, N'.d@it.invalid'), 1, 1, N'sin-credencial'),
                       (@uE, @tipoCC, @base + 1, N'Aviso', N'IT', N'Est', N'IT', CONCAT(@tag, N'.e@it.invalid'), 1, 1, N'sin-credencial');
                INSERT INTO dbo.Docente (id, usuario) VALUES (@d, @uD);
                INSERT INTO dbo.Estudiante (id, usuario) VALUES (@e, @uE);
                INSERT INTO dbo.PeriodoAcademico (id, institucion, nombre, codigo, fechaInicio, fechaFin, anio)
                VALUES (@per, @idInst, @tag, 3900000 + ABS(CHECKSUM(NEWID()) % 90000), DATEADD(DAY, -60, @hoy), DATEADD(DAY, 60, @hoy), YEAR(@hoy));
                INSERT INTO dbo.Grupo (id, asignatura, periodoAcademico, codigo, nombre, cantidadEstudiantes, cantidadEstudiantesFinalizaron,
                                       cantidadEstudiantesCancelaronVoluntadPropia, cantidadEstudiantesCancelaronAutomaticamente, docente)
                VALUES (@g, @idAsig, @per, 960000 + ABS(CHECKSUM(NEWID()) % 9999), @tag, 10, 0, 0, 0, @d);
                INSERT INTO dbo.EstudianteGrupo (id, estado, estudiante, grupo) VALUES (@eg, @estadoA, @e, @g);
                DECLARE @n INT = 1;
                WHILE @n <= 3
                BEGIN
                    DECLARE @s UNIQUEIDENTIFIER = NEWID(), @t UNIQUEIDENTIFIER = NEWID(), @a UNIQUEIDENTIFIER = NEWID();
                    DECLARE @ini DATETIME2(7) = DATEADD(HOUR, -(24 * (@n + 1)), SYSUTCDATETIME());
                    INSERT INTO dbo.Sesion (id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio, fechaHoraFin, procedenciaTemporal)
                    VALUES (@s, CONCAT(@tag, N' S', @n), @n, CONCAT(N'AV', RIGHT(@tag, 4), @n), @n, @g, @ini, DATEADD(HOUR, 2, @ini), N'UTC_V2');
                    INSERT INTO dbo.TomaAsistencia (id, sesion, numero, modo, estado, esImplicita, docenteApertura, fechaAperturaUtc,
                                                    fechaCierreUtc, idCorrelacionApertura)
                    VALUES (@t, @s, 1, @modo, @cerrada, 1, @d, DATEADD(MINUTE, 10, @ini), DATEADD(MINUTE, 20, @ini), NEWID());
                    INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (@a, @eg, @s);
                    INSERT INTO dbo.DetalleAsistencia (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin, toma)
                    VALUES (NEWID(), 900000 + ABS(CHECKSUM(NEWID()) % 90000), @a, 0, @sjc, @ini, DATEADD(HOUR, 2, @ini), @t);
                    SET @n = @n + 1;
                END
                """, etiqueta);
        return etiqueta;
    }
}