package co.edu.uco.asistenciasuco.application.features.alertainasistencia;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AvisoAlertasInasistenciaService;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.usecase.impl.ConsultarAlertasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.usecase.impl.EvaluarAlertasInasistenciaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.usecase.impl.ResolverAudienciaRealtimeUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadAlerta;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.NovedadResolutores;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.PaginaAlertas;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.Persona;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoEvaluacion;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.ResultadoResolutores;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.Resolutor;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (contrato 3.4, 3.5, 3.6.4, 3.9.1): avisos del motor, consulta del estudiante, job y audiencias del SSE. */
class AlertaInasistenciaUseCasesTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID EG = UUID.randomUUID();
    private static final UUID USUARIO_EST = UUID.randomUUID();
    private static final UUID USUARIO_DOC = UUID.randomUUID();

    private AlertaInasistenciaRepositoryPort repositorio;
    private NotificacionPort notificaciones;
    private RealtimePublisherPort realtime;
    private AvisoAlertasInasistenciaService servicio;
    private final co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort mensajes = mock(co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort.class);

    @BeforeEach
    void preparar() {
        repositorio = mock(AlertaInasistenciaRepositoryPort.class);
        notificaciones = mock(NotificacionPort.class);
        realtime = mock(RealtimePublisherPort.class);
        servicio = new AvisoAlertasInasistenciaService(repositorio, notificaciones, realtime);
    }

    private static NovedadAlerta novedad(final String tipo, final boolean principal, final UUID revision) {
        return new NovedadAlerta(UUID.randomUUID(), tipo, "PERDIDA".equals(tipo) ? 2 : 1, principal, new BigDecimal("21.50"), new BigDecimal("20.00"),
                new BigDecimal("20.00"), new BigDecimal("5"), new BigDecimal("20"), EG, GRUPO, 1, "G01", "Bases de datos",
                "PERDIDA".equals(tipo) ? Notificacion.TIPO_ALERTA_PERDIDA_INASISTENCIA : Notificacion.TIPO_ALERTA_RIESGO_INASISTENCIA,
                new Persona(USUARIO_EST, "Est Uno", "est@x.co"), new Persona(USUARIO_DOC, "Doc Uno", "doc@x.co"), revision);
    }

    private static ResultadoEvaluacion resultado(final NovedadAlerta... novedades) {
        return new ResultadoEvaluacion(novedades.length, 0, 0, 0, List.of(novedades));
    }

    @Test
    void sin_novedades_no_avisa_ni_publica() {
        when(repositorio.evaluar(GRUPO, null)).thenReturn(resultado());
        servicio.evaluarYAvisar(GRUPO, null);
        verifyNoInteractions(notificaciones, realtime);
    }

    @Test
    void la_alerta_principal_avisa_al_estudiante_y_publica_el_evento_con_destinatario_fuera_del_payload() {
        when(repositorio.evaluar(GRUPO, EG)).thenReturn(resultado(novedad("ADVERTENCIA", true, null)));
        servicio.evaluarYAvisar(GRUPO, EG);

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_ALERTA_RIESGO_INASISTENCIA, aviso.getValue().tipo());
        assertEquals(USUARIO_EST, aviso.getValue().destinatarios().getFirst().idUsuario());
        assertEquals("20.00", aviso.getValue().datos().get("limite"));
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("ALERTA_INASISTENCIA", evento.getValue().type());
        assertEquals(Set.of(USUARIO_EST), evento.getValue().destinatariosUsuario());
        assertEquals(GRUPO.toString(), evento.getValue().payload().get("grupo"));
        assertFalse(evento.getValue().payload().toString().contains(USUARIO_EST.toString()));
    }

    @Test
    void una_alerta_no_principal_solo_publica_el_evento_y_una_revision_avisa_al_docente() {
        final UUID revision = UUID.randomUUID();
        when(repositorio.evaluar(GRUPO, null)).thenReturn(resultado(novedad("ADVERTENCIA", false, null), novedad("PERDIDA", true, revision)));
        servicio.evaluarYAvisar(GRUPO, null);

        final ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisos.capture());
        assertEquals(List.of(Notificacion.TIPO_ALERTA_PERDIDA_INASISTENCIA, Notificacion.TIPO_REVISION_CANCELACION_PENDIENTE),
                avisos.getAllValues().stream().map(Notificacion::tipo).toList());
        final ArgumentCaptor<RealtimeEvent> eventos = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, times(3)).publish(eventos.capture());
        assertEquals(List.of("ALERTA_INASISTENCIA", "ALERTA_INASISTENCIA", "REVISION_CANCELACION_CREADA"),
                eventos.getAllValues().stream().map(RealtimeEvent::type).toList());
        assertEquals(Set.of(USUARIO_DOC), eventos.getAllValues().get(2).destinatariosUsuario());
    }

    @Test
    void los_fallos_del_motor_del_correo_y_del_canal_no_se_propagan() {
        when(repositorio.evaluar(GRUPO, null)).thenThrow(new IllegalStateException("db"));
        servicio.evaluarYAvisar(GRUPO, null);

        when(repositorio.evaluar(GRUPO, EG)).thenReturn(resultado(novedad("PERDIDA", true, UUID.randomUUID())));
        doThrow(new IllegalStateException("smtp")).when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());
        servicio.evaluarYAvisar(GRUPO, EG);
        verify(notificaciones, times(2)).notificar(any());

        when(repositorio.evaluar(GRUPO, UUID.fromString("00000000-0000-0000-0000-000000000009"))).thenReturn(null);
        servicio.evaluarYAvisar(GRUPO, UUID.fromString("00000000-0000-0000-0000-000000000009"));
    }

    @Test
    void evaluar_un_periodo_recorre_sus_grupos_y_tolera_el_fallo_del_listado() {
        final UUID periodo = UUID.randomUUID();
        final UUID otro = UUID.randomUUID();
        when(repositorio.consultarGruposPendientes(null, periodo)).thenReturn(List.of(GRUPO, otro));
        when(repositorio.evaluar(any(), any())).thenReturn(resultado());
        servicio.evaluarPeriodoYAvisar(periodo);
        verify(repositorio).evaluar(GRUPO, null);
        verify(repositorio).evaluar(otro, null);

        when(repositorio.consultarGruposPendientes(null, periodo)).thenThrow(new IllegalStateException("db"));
        servicio.evaluarPeriodoYAvisar(periodo);
        EvaluadorAlertasInasistencia.NINGUNO.evaluarYAvisar(GRUPO, null);
        EvaluadorAlertasInasistencia.NINGUNO.evaluarPeriodoYAvisar(periodo);
    }

    @Test
    void los_constructores_exigen_sus_colaboradores() {
        assertThrows(NullPointerException.class, () -> new AvisoAlertasInasistenciaService(null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new AvisoAlertasInasistenciaService(repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new AvisoAlertasInasistenciaService(repositorio, notificaciones, null));
    }

    @Test
    void consultar_alertas_aplica_los_valores_por_defecto_de_paginacion() {
        final ConsultarAlertasEstudianteUseCaseImpl useCase = new ConsultarAlertasEstudianteUseCaseImpl(repositorio, mensajes);
        final PaginaAlertas pagina = new PaginaAlertas(List.of(), 0);
        when(repositorio.consultarAlertasEstudiante(USUARIO_EST, 1, 20, false, true, null)).thenReturn(pagina);
        when(repositorio.consultarAlertasEstudiante(USUARIO_EST, 3, 5, true, false, GRUPO)).thenReturn(pagina);

        assertEquals(pagina, useCase.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO_EST, null, null, false, true, null)));
        assertEquals(pagina, useCase.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO_EST, 3, 5, true, false, GRUPO)));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarAlertasEstudianteUseCaseImpl(null, mensajes));
        assertThrows(NullPointerException.class, () -> new ConsultarAlertasEstudianteUseCaseImpl(repositorio, null));
    }

    @Test
    void los_mensajes_de_las_alertas_salen_del_catalogo_y_ante_su_caida_del_texto_de_respaldo() {
        final ConsultarAlertasEstudianteUseCaseImpl useCase = new ConsultarAlertasEstudianteUseCaseImpl(repositorio, mensajes);
        final var riesgo = fila("ADVERTENCIA", "ALR_010");
        final var perdida = fila("PERDIDA", "ALR_011");
        when(repositorio.consultarAlertasEstudiante(USUARIO_EST, 1, 20, false, false, null)).thenReturn(new PaginaAlertas(List.of(riesgo, perdida), 2));
        when(mensajes.getUserMessage("ALR_010", "16.00", "Bases", "20.00")).thenReturn("Llevas 16.00% (catalogo).");
        when(mensajes.getUserMessage("ALR_011", "16.00", "Bases", "15.00")).thenThrow(new IllegalStateException("catalogo"));

        final PaginaAlertas pagina = useCase.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO_EST, null, null, false, false, null));
        assertEquals("Llevas 16.00% (catalogo).", pagina.filas().get(0).mensaje());
        assertEquals("Alcanzaste 16.00% de inasistencia en Bases (límite 15.00%). Tu docente revisará tu caso.", pagina.filas().get(1).mensaje());

        when(mensajes.getUserMessage("ALR_010", "16.00", "Bases", "20.00")).thenThrow(new IllegalStateException("catalogo"));
        assertEquals("Llevas 16.00% de inasistencia en Bases. Si llegas al 20.00% podrías perder la asignatura.",
                useCase.execute(new ConsultarAlertasEstudianteInputPort.Comando(USUARIO_EST, null, null, false, false, null)).filas().getFirst().mensaje());
    }

    private static co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila fila(final String tipo, final String codigo) {
        return new co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AlertaFila(UUID.randomUUID(), tipo, tipo, "VIGENTE", codigo,
                new BigDecimal("16.00"), new BigDecimal("16.00"), new BigDecimal("15.00"), new BigDecimal("20.00"), null, null, null, false, null, GRUPO, 1,
                "G01", UUID.randomUUID(), "Bases", "A", null);
    }

    @Test
    void el_job_evalua_los_grupos_pendientes_y_avisa_las_reasignaciones_sin_que_un_fallo_detenga_la_pasada() {
        final RevisionCancelacionRepositoryPort revisiones = mock(RevisionCancelacionRepositoryPort.class);
        final AvisoRevisionCancelacionService avisos = new AvisoRevisionCancelacionService(revisiones, notificaciones, realtime);
        final EvaluadorAlertasInasistencia evaluador = mock(EvaluadorAlertasInasistencia.class);
        final EvaluarAlertasInasistenciaAutomaticoUseCaseImpl useCase = new EvaluarAlertasInasistenciaAutomaticoUseCaseImpl(repositorio, evaluador, avisos);
        final UUID revision = UUID.randomUUID();
        final NovedadResolutores novedad = new NovedadResolutores(revision, GRUPO, "G01", "Bases", EG, "COORDINADOR", "REASIGNAR", new BigDecimal("22"),
                new BigDecimal("20"), "motivo", "Est", "Doc", USUARIO_DOC, USUARIO_EST,
                List.of(new Resolutor(UUID.randomUUID(), "Coord", "c@x.co", "COORDINADOR", false)));
        when(repositorio.consultarGruposPendientes(null, null)).thenReturn(List.of(GRUPO, UUID.randomUUID()));
        when(repositorio.actualizarResolutores(null)).thenReturn(new ResultadoResolutores(1, 0, 0, List.of(novedad)));
        when(revisiones.consultarDestinatarios(any(), any())).thenReturn(List.of());

        assertEquals(2, useCase.execute());
        verify(evaluador, times(2)).evaluarYAvisar(any(), any());
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_PROPUESTA_CANCELACION, aviso.getValue().tipo());
        // DEC-065 (1): la reasignacion es siempre a otro coordinador; ya no hay texto de escalamiento al decano.
        assertEquals(false, aviso.getValue().datos().containsKey("escalamiento"));
        verify(realtime).publish(any());

        when(repositorio.consultarGruposPendientes(null, null)).thenThrow(new IllegalStateException("db"));
        when(repositorio.actualizarResolutores(null)).thenThrow(new IllegalStateException("db"));
        assertEquals(0, useCase.execute());

        // sin resolutor posible: solo el evento
        org.mockito.Mockito.doReturn(List.of()).when(repositorio).consultarGruposPendientes(null, null);
        org.mockito.Mockito.doReturn(new ResultadoResolutores(1, 0, 0, List.of(
                new NovedadResolutores(revision, GRUPO, "G01", "Bases", EG, null, "REASIGNAR", null, null, "m", "E", "D", null, USUARIO_EST, List.of()))))
                .when(repositorio).actualizarResolutores(null);
        useCase.execute();
        verify(notificaciones, times(1)).notificar(any());
        assertThrows(NullPointerException.class, () -> new EvaluarAlertasInasistenciaAutomaticoUseCaseImpl(null, evaluador, avisos));
    }

    @Test
    void resolver_audiencia_traduce_las_tres_audiencias_de_la_db() {
        final RealtimeAudienciaQueryPort consulta = mock(RealtimeAudienciaQueryPort.class);
        final ResolverAudienciaRealtimeUseCaseImpl useCase = new ResolverAudienciaRealtimeUseCaseImpl(consulta);
        when(consulta.consultar(USUARIO_EST, GRUPO)).thenReturn(new RealtimeAudienciaQueryPort.Audiencias(false, true, false));
        when(consulta.consultar(USUARIO_DOC, GRUPO)).thenReturn(new RealtimeAudienciaQueryPort.Audiencias(false, false, false));

        final AudienciaRealtimeGrupo estudiante = useCase.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO_EST, GRUPO));
        assertTrue(estudiante.estudianteGrupo() && estudiante.tieneAlguna() && !estudiante.docenteTitular() && !estudiante.ambitoGrupo());
        assertFalse(useCase.execute(new ResolverAudienciaRealtimeInputPort.Comando(USUARIO_DOC, GRUPO)).tieneAlguna());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new ResolverAudienciaRealtimeUseCaseImpl(null));
        verify(consulta, never()).consultar(any(), org.mockito.ArgumentMatchers.eq(UUID.randomUUID()));
    }
}
