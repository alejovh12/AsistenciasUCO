package co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.MotivoCancelacion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudRechazada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DEC-065 (2): el coordinador rechaza la solicitud. UNA notificacion al docente con motivo y observacion, evento
 * {@code SESION_CANCELADA} (tipo COORDINADOR) y mejor esfuerzo posterior al commit.
 */
class RechazarSolicitudSesionUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COORDINADOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 13, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 10, 15, 0);
    private static final LocalDateTime CANCELADA = LocalDateTime.of(2026, 11, 9, 14, 0);
    private static final Comando COMANDO = new Comando(SESION, "DUPLICADA", "  Ya existe la del lunes  ", COORDINADOR);
    private static final String MENSAJE = "La solicitud de sesión fue rechazada.";

    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final RechazarSolicitudSesionUseCaseImpl useCase =
            new RechazarSolicitudSesionUseCaseImpl(sesiones, repositorio, notificaciones, realtime);

    @Test
    void rechaza_avisa_al_docente_con_motivo_y_observacion_y_publica_la_cancelacion_del_coordinador() {
        prepararRechazo();
        when(repositorio.consultarDestinatariosSolicitud(SESION, COORDINADOR)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente"),
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez")));
        when(repositorio.consultarMotivosCancelacion(COORDINADOR)).thenReturn(List.of(
                new MotivoCancelacion("OTRO", "Otro motivo"), new MotivoCancelacion("DUPLICADA", "Sesión duplicada")));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2")));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 12, true, true), resultado);

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_SESION_SOLICITUD_RECHAZADA_DOCENTE, aviso.getValue().tipo());
        assertEquals("Solicitud rechazada: Calculo (10/11/2026)", aviso.getValue().asunto());
        assertEquals("Hola Maria Docente. El coordinador del programa rechazó su solicitud de sesión extraordinaria de Calculo del "
                + "grupo G01 de Calculo para el 10/11/2026 de 08:00 a 10:00. Motivo: Sesión duplicada. Ya existe la del lunes",
                aviso.getValue().mensaje());
        assertEquals(1, aviso.getValue().destinatarios().size());
        assertEquals("DOCENTE", aviso.getValue().destinatarios().getFirst().rol());
        assertEquals(Map.of("docente", "Maria Docente", "coordinador", "El coordinador del programa", "asignatura", "Calculo",
                "grupo", "G01 de Calculo", "fecha", "10/11/2026", "horaInicio", "08:00", "horaFin", "10:00",
                "idSesion", SESION.toString(), "motivo", "Sesión duplicada", "observacion", "Ya existe la del lunes"),
                aviso.getValue().datos());

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("SESION_CANCELADA", evento.getValue().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "tipo", "COORDINADOR",
                "fechaHoraInicioUtc", "2026-11-10T13:00:00Z"), evento.getValue().payload());
    }

    @Test
    void sin_observacion_ni_motivo_en_el_catalogo_usa_el_codigo_y_deja_la_observacion_vacia() {
        when(repositorio.rechazarSolicitud(SESION, "OTRO", null, COORDINADOR)).thenReturn(new SolicitudRechazada(SESION, GRUPO, CANCELADA, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of(destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente")));
        when(repositorio.consultarMotivosCancelacion(COORDINADOR)).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));

        useCase.execute(new Comando(SESION, "OTRO", null, COORDINADOR));

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals("OTRO", aviso.getValue().datos().get("motivo"));
        assertEquals("", aviso.getValue().datos().get("observacion"));
        assertEquals("Hola Maria Docente. El coordinador del programa rechazó su solicitud de sesión extraordinaria de Calculo del grupo "
                + "G01 de Calculo para el 10/11/2026 de 08:00 a 10:00. Motivo: OTRO.", aviso.getValue().mensaje());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertFalse(evento.getValue().payload().containsKey("fechaHoraInicioUtc"));
    }

    @Test
    void si_el_catalogo_de_motivos_falla_o_el_codigo_es_nulo_usa_el_codigo_recibido() {
        when(repositorio.rechazarSolicitud(any(), any(), any(), any())).thenReturn(new SolicitudRechazada(SESION, GRUPO, CANCELADA, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of(destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente")));
        when(repositorio.consultarMotivosCancelacion(COORDINADOR)).thenThrow(new IllegalStateException("db"));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());

        useCase.execute(COMANDO);
        useCase.execute(new Comando(SESION, null, null, COORDINADOR));

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, org.mockito.Mockito.times(2)).notificar(aviso.capture());
        assertEquals("DUPLICADA", aviso.getAllValues().get(0).datos().get("motivo"));
        assertEquals("", aviso.getAllValues().get(1).datos().get("motivo"));
    }

    @Test
    void una_sesion_sin_hora_de_inicio_publica_el_evento_sin_hora() {
        when(repositorio.rechazarSolicitud(any(), any(), any(), any())).thenReturn(new SolicitudRechazada(SESION, GRUPO, CANCELADA, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion extra", 5, "COD5", 1, "01", "G01", null, null, "UTC_V2")));

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 0, false, true), useCase.execute(COMANDO));
        verify(notificaciones, never()).notificar(any());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertFalse(evento.getValue().payload().containsKey("fechaHoraInicioUtc"));
    }

    @Test
    void los_fallos_posteriores_al_commit_no_cambian_la_respuesta() {
        prepararRechazo();
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of(destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente")));
        when(repositorio.consultarMotivosCancelacion(COORDINADOR)).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenThrow(new IllegalStateException("db"));
        doThrow(new IllegalStateException("smtp")).when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 12, false, false), useCase.execute(COMANDO));
    }

    @Test
    void si_no_hay_docente_destinatario_no_envia_el_correo() {
        prepararRechazo();
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of(destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez")));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());

        assertEquals(false, useCase.execute(COMANDO).docenteNotificado());
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void los_rechazos_de_la_db_se_propagan_sin_avisos_ni_evento() {
        when(repositorio.rechazarSolicitud(any(), any(), any(), any()))
                .thenThrow(new ConflictException(SesionErrorCode.SES_024))
                .thenThrow(new ValidationException(SesionErrorCode.SES_019));

        assertThrows(ConflictException.class, () -> useCase.execute(COMANDO));
        assertThrows(ValidationException.class, () -> useCase.execute(COMANDO));
        verifyNoInteractions(notificaciones, realtime);
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new RechazarSolicitudSesionUseCaseImpl(null, repositorio, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new RechazarSolicitudSesionUseCaseImpl(sesiones, null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new RechazarSolicitudSesionUseCaseImpl(sesiones, repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new RechazarSolicitudSesionUseCaseImpl(sesiones, repositorio, notificaciones, null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private void prepararRechazo() {
        when(repositorio.rechazarSolicitud(SESION, "DUPLICADA", "  Ya existe la del lunes  ", COORDINADOR))
                .thenReturn(new SolicitudRechazada(SESION, GRUPO, CANCELADA, 12, MENSAJE));
    }

    private static DestinatarioSesionManual destinatario(final String tipo, final String nombre) {
        return new DestinatarioSesionManual(tipo, UUID.randomUUID(), nombre, nombre + "@uco.edu.co", "Calculo", "G01 de Calculo",
                "Maria Docente", INICIO, FIN);
    }

    private static SesionProcedenciaRepositoryProjection sesion(final String procedencia) {
        return new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "Sesion extra", 5, "COD5", 1, "01", "G01", INICIO, FIN, procedencia);
    }
}
