package co.edu.uco.asistenciasuco.application.features.revisioncancelacion;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.usecase.impl.ConsultarRevisionesCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.usecase.impl.ConsultarRevisionesDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.usecase.impl.DecidirRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.usecase.impl.ResolverRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.DestinatarioRevision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.PaginaRevisiones;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoDecision;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.ResultadoResolucion;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (contrato 3.5, 3.6.4, 3.9.1): decidir, resolver, bandejas y avisos de la revision de cancelacion. */
class RevisionCancelacionUseCasesTest {

    private static final UUID REVISION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID EG = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID RESOLUTOR = UUID.randomUUID();

    private RevisionCancelacionRepositoryPort repositorio;
    private NotificacionPort notificaciones;
    private RealtimePublisherPort realtime;
    private AvisoRevisionCancelacionService avisos;

    @BeforeEach
    void preparar() {
        repositorio = mock(RevisionCancelacionRepositoryPort.class);
        notificaciones = mock(NotificacionPort.class);
        realtime = mock(RealtimePublisherPort.class);
        avisos = new AvisoRevisionCancelacionService(repositorio, notificaciones, realtime);
        when(repositorio.consultarDestinatarios(any(), any())).thenReturn(List.of(
                destinatario("ESTUDIANTE", "ESTUDIANTE", ESTUDIANTE), destinatario("DOCENTE", "DOCENTE", DOCENTE),
                destinatario("RESOLUTOR", "COORDINADOR", RESOLUTOR)));
    }

    private static DestinatarioRevision destinatario(final String rol, final String perfil, final UUID usuario) {
        return new DestinatarioRevision(rol, perfil, usuario, rol + " Uno", rol + "@x.co", UUID.randomUUID(), false, GRUPO, 1, "G01", "Bases",
                "Est Uno", "Doc Uno", "PROPUESTA", new BigDecimal("22"), new BigDecimal("20"), "motivo docente", "motivo resolucion", "Coord Uno");
    }

    private static ResultadoDecision decision(final String plantilla, final boolean sinCambios, final boolean escalamiento) {
        return new ResultadoDecision("ok", REVISION, "PENDIENTE_DOCENTE", plantilla == null ? "MANTENIDA" : "PROPUESTA", plantilla == null ? "MANTENER" : "PROPONER",
                sinCambios, GRUPO, EG, ESTUDIANTE, new BigDecimal("22"), plantilla, plantilla == null ? null : (escalamiento ? "DECANO" : "COORDINADOR"), escalamiento);
    }

    private static ResultadoResolucion resolucion(final String plantilla, final boolean sinCambios, final boolean cambio) {
        return new ResultadoResolucion("ok", REVISION, "PROPUESTA", cambio ? "APROBADA" : "RECHAZADA", cambio ? "APROBAR" : "RECHAZAR", sinCambios, cambio,
                GRUPO, EG, ESTUDIANTE, UUID.randomUUID(), "COORDINADOR", new BigDecimal("22"), plantilla);
    }

    @Test
    void mantener_publica_el_evento_al_docente_sin_correo() {
        final ResultadoDecision r = decision(null, false, false);
        when(repositorio.decidir(REVISION, "MANTENER", "motivo valido", DOCENTE)).thenReturn(r);
        assertEquals(r, new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos)
                .execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "MANTENER", "motivo valido", DOCENTE)));

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("REVISION_CANCELACION_DECIDIDA", evento.getValue().type());
        assertEquals(Set.of(DOCENTE), evento.getValue().destinatariosUsuario());
        verifyNoInteractions(notificaciones);
    }

    @Test
    void proponer_avisa_solo_a_los_resolutores_y_el_evento_incluye_al_docente_y_a_los_resolutores() {
        when(repositorio.decidir(REVISION, "PROPONER", "motivo valido", DOCENTE)).thenReturn(decision(Notificacion.TIPO_PROPUESTA_CANCELACION, false, false));
        new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos)
                .execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "PROPONER", "motivo valido", DOCENTE));

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_PROPUESTA_CANCELACION, aviso.getValue().tipo());
        assertEquals(RESOLUTOR, aviso.getValue().destinatarios().getFirst().idUsuario());
        assertFalse(aviso.getValue().datos().containsKey("escalamiento"));
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals(Set.of(DOCENTE, RESOLUTOR), evento.getValue().destinatariosUsuario());
    }

    @Test
    void proponer_nunca_informa_escalamiento_al_decano_dec_065() {
        // DEC-065 (1): se retira el escalamiento; aunque la DB (compatibilidad) devolviera el indicador, el aviso no lo menciona.
        when(repositorio.decidir(REVISION, "PROPONER", "motivo valido", DOCENTE)).thenReturn(decision(Notificacion.TIPO_PROPUESTA_CANCELACION, false, true));
        new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos)
                .execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "PROPONER", "motivo valido", DOCENTE));
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertFalse(aviso.getValue().datos().containsKey("escalamiento"));
        assertFalse(aviso.getValue().mensaje().contains("coordinador disponible"));
    }

    @Test
    void una_decision_sin_cambios_no_avisa_y_los_fallos_de_aviso_no_se_propagan() {
        when(repositorio.decidir(any(), any(), any(), any())).thenReturn(decision(null, true, false));
        new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "MANTENER", "m", DOCENTE));
        verifyNoInteractions(realtime, notificaciones);

        when(repositorio.decidir(any(), any(), any(), any())).thenReturn(decision(Notificacion.TIPO_PROPUESTA_CANCELACION, false, false));
        doThrow(new IllegalStateException("smtp")).when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());
        new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "PROPONER", "m", DOCENTE));

        when(repositorio.consultarDestinatarios(any(), any())).thenThrow(new IllegalStateException("db"));
        new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new DecidirRevisionCancelacionInputPort.Comando(REVISION, "PROPONER", "m", DOCENTE));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos);
        assertThrows(NullPointerException.class, () -> new DecidirRevisionCancelacionUseCaseImpl(null, avisos));
        assertThrows(NullPointerException.class, () -> new DecidirRevisionCancelacionUseCaseImpl(repositorio, null));
    }

    @Test
    void aprobar_avisa_al_estudiante_y_al_docente_y_el_evento_llega_a_los_tres() {
        when(repositorio.resolver(REVISION, "APROBAR", "motivo valido", RESOLUTOR)).thenReturn(resolucion(Notificacion.TIPO_CANCELACION_APROBADA, false, true));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos)
                .execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "APROBAR", "motivo valido", RESOLUTOR));

        final ArgumentCaptor<Notificacion> avisosEnviados = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisosEnviados.capture());
        assertEquals(Set.of(ESTUDIANTE, DOCENTE), Set.copyOf(avisosEnviados.getAllValues().stream().map(n -> n.destinatarios().getFirst().idUsuario()).toList()));
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("REVISION_CANCELACION_RESUELTA", evento.getValue().type());
        assertEquals(Set.of(ESTUDIANTE, DOCENTE, RESOLUTOR), evento.getValue().destinatariosUsuario());
        assertEquals(true, evento.getValue().payload().get("cambioMatricula"));
    }

    @Test
    void rechazar_avisa_solo_al_docente_y_el_estudiante_no_recibe_el_evento() {
        when(repositorio.resolver(REVISION, "RECHAZAR", "motivo valido", RESOLUTOR)).thenReturn(resolucion(Notificacion.TIPO_CANCELACION_RECHAZADA, false, false));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos)
                .execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "RECHAZAR", "motivo valido", RESOLUTOR));
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(DOCENTE, aviso.getValue().destinatarios().getFirst().idUsuario());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals(Set.of(DOCENTE, RESOLUTOR), evento.getValue().destinatariosUsuario());
    }

    @Test
    void resolver_sin_cambios_y_sin_plantilla_no_envia_correos_y_tolera_fallos() {
        when(repositorio.resolver(any(), any(), any(), any())).thenReturn(resolucion(null, true, true));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "APROBAR", "m", RESOLUTOR));
        verifyNoInteractions(realtime, notificaciones);

        when(repositorio.resolver(any(), any(), any(), any())).thenReturn(resolucion(null, false, false));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "RECHAZAR", "m", RESOLUTOR));
        verify(realtime).publish(any());
        verifyNoInteractions(notificaciones);

        when(repositorio.consultarDestinatarios(any(), any())).thenThrow(new IllegalStateException("db"));
        new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(new ResolverRevisionCancelacionInputPort.Comando(REVISION, "RECHAZAR", "m", RESOLUTOR));
        assertThrows(NullPointerException.class, () -> new ResolverRevisionCancelacionUseCaseImpl(null, avisos));
        assertThrows(NullPointerException.class, () -> new ResolverRevisionCancelacionUseCaseImpl(repositorio, null));
        assertThrows(NullPointerException.class, () -> new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(null));
        assertThrows(NullPointerException.class, () -> new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos).execute(null));
    }

    @Test
    void las_bandejas_aplican_los_valores_por_defecto() {
        final PaginaRevisiones pagina = new PaginaRevisiones(List.of(), 0);
        when(repositorio.consultarCoordinador(RESOLUTOR, 1, 20, false, null, null, null)).thenReturn(pagina);
        when(repositorio.consultarCoordinador(RESOLUTOR, 2, 10, true, "PROPUESTA", GRUPO, EG)).thenReturn(pagina);
        when(repositorio.consultarDecano(RESOLUTOR, 1, 20, false, null, null)).thenReturn(pagina);
        when(repositorio.consultarDecano(RESOLUTOR, 2, 10, true, "PROPUESTA", GRUPO)).thenReturn(pagina);
        final ConsultarRevisionesCoordinadorUseCaseImpl coordinador = new ConsultarRevisionesCoordinadorUseCaseImpl(repositorio);
        final ConsultarRevisionesDecanoUseCaseImpl decano = new ConsultarRevisionesDecanoUseCaseImpl(repositorio);

        assertEquals(pagina, coordinador.execute(new ConsultarRevisionesCoordinadorInputPort.Comando(RESOLUTOR, null, null, false, null, null, null)));
        assertEquals(pagina, coordinador.execute(new ConsultarRevisionesCoordinadorInputPort.Comando(RESOLUTOR, 2, 10, true, "PROPUESTA", GRUPO, EG)));
        assertEquals(pagina, decano.execute(new ConsultarRevisionesDecanoInputPort.Comando(RESOLUTOR, null, null, false, null, null)));
        assertEquals(pagina, decano.execute(new ConsultarRevisionesDecanoInputPort.Comando(RESOLUTOR, 2, 10, true, "PROPUESTA", GRUPO)));
        assertThrows(NullPointerException.class, () -> coordinador.execute(null));
        assertThrows(NullPointerException.class, () -> decano.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarRevisionesCoordinadorUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarRevisionesDecanoUseCaseImpl(null));
    }
}
