package co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudAprobada;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DEC-065 (2): el coordinador aprueba la solicitud. Una notificacion agrupada a los estudiantes (tipo existente), una al docente
 * ({@code SESION_SOLICITUD_APROBADA_DOCENTE}) y el evento {@code SESION_MANUAL_CREADA}; todo post-commit y de mejor esfuerzo.
 */
class AprobarSolicitudSesionUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COORDINADOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 13, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 10, 15, 0);
    private static final Comando COMANDO = new Comando(SESION, "Adelante", COORDINADOR);
    private static final String MENSAJE = "La solicitud de sesión fue aprobada.";

    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final AprobarSolicitudSesionUseCaseImpl useCase =
            new AprobarSolicitudSesionUseCaseImpl(sesiones, repositorio, notificaciones, realtime);

    @Test
    void aprueba_avisa_a_estudiantes_y_docente_en_un_correo_cada_uno_y_publica_el_evento() {
        when(repositorio.aprobarSolicitud(SESION, "Adelante", COORDINADOR)).thenReturn(new SolicitudAprobada(SESION, GRUPO, INICIO, 2, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(SESION, COORDINADOR)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente"),
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez"),
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Luis Gomez")));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2")));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 2, 2, true, true), resultado);

        final ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisos.capture());
        final Notificacion alEstudiante = avisos.getAllValues().get(0);
        assertEquals(Notificacion.TIPO_SESION_MANUAL_CREADA_ESTUDIANTE, alEstudiante.tipo());
        assertEquals(List.of("Ana Perez", "Luis Gomez"), alEstudiante.destinatarios().stream().map(Notificacion.Destinatario::nombre).toList());
        assertEquals("estudiantes del grupo", alEstudiante.datos().get("estudiante"));

        final Notificacion alDocente = avisos.getAllValues().get(1);
        assertEquals(Notificacion.TIPO_SESION_SOLICITUD_APROBADA_DOCENTE, alDocente.tipo());
        assertEquals("Solicitud aprobada: Calculo (10/11/2026)", alDocente.asunto());
        assertEquals("Hola Maria Docente. El coordinador del programa aprobó su solicitud de sesión extraordinaria de Calculo del grupo "
                + "G01 de Calculo para el 10/11/2026 de 08:00 a 10:00. Ya puede tomar asistencia en ella y los estudiantes fueron "
                + "notificados.", alDocente.mensaje());
        assertEquals(1, alDocente.destinatarios().size());
        assertEquals("DOCENTE", alDocente.destinatarios().getFirst().rol());
        assertEquals(Map.of("docente", "Maria Docente", "coordinador", "El coordinador del programa", "asignatura", "Calculo",
                "grupo", "G01 de Calculo", "fecha", "10/11/2026", "horaInicio", "08:00", "horaFin", "10:00",
                "idSesion", SESION.toString()), alDocente.datos());

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("SESION_MANUAL_CREADA", evento.getValue().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "fechaHoraInicio", "2026-11-10T13:00:00Z",
                "docente", "Maria Docente"), evento.getValue().payload());
    }

    @Test
    void si_no_hay_destinatarios_no_envia_correos_pero_publica_el_evento_con_la_hora_almacenada() {
        when(repositorio.aprobarSolicitud(any(), any(), any())).thenReturn(new SolicitudAprobada(SESION, GRUPO, INICIO, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(SESION, COORDINADOR)).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 0, 0, false, true), useCase.execute(COMANDO));
        verify(notificaciones, never()).notificar(any());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("2026-11-10T13:00", evento.getValue().payload().get("fechaHoraInicio"));
        assertEquals("", evento.getValue().payload().get("docente"));
    }

    @Test
    void sin_hora_almacenada_ni_sesion_legible_el_evento_lleva_la_hora_vacia() {
        when(repositorio.aprobarSolicitud(any(), any(), any())).thenReturn(new SolicitudAprobada(SESION, GRUPO, null, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenThrow(new IllegalStateException("db"));

        useCase.execute(COMANDO);

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("", evento.getValue().payload().get("fechaHoraInicio"));
    }

    @Test
    void un_horario_indeterminado_publica_la_hora_local_sin_zona() {
        when(repositorio.aprobarSolicitud(any(), any(), any())).thenReturn(new SolicitudAprobada(SESION, GRUPO, INICIO, 0, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of());
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));

        useCase.execute(COMANDO);

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("2026-11-10T13:00", evento.getValue().payload().get("fechaHoraInicio"));
    }

    @Test
    void los_fallos_posteriores_al_commit_no_cambian_la_respuesta() {
        when(repositorio.aprobarSolicitud(any(), any(), any())).thenReturn(new SolicitudAprobada(SESION, GRUPO, INICIO, 2, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_DOCENTE, "Maria Docente"),
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez")));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2")));
        doThrow(new IllegalStateException("smtp")).when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 2, 0, false, false), useCase.execute(COMANDO));
    }

    @Test
    void si_no_se_pueden_consultar_los_destinatarios_igual_publica_el_evento() {
        when(repositorio.aprobarSolicitud(any(), any(), any())).thenReturn(new SolicitudAprobada(SESION, GRUPO, INICIO, 2, MENSAJE));
        when(repositorio.consultarDestinatariosSolicitud(any(), any())).thenThrow(new IllegalStateException("db"));
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2")));

        assertEquals(new Resultado(SESION, GRUPO, MENSAJE, 2, 0, false, true), useCase.execute(COMANDO));
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void los_rechazos_de_la_db_se_propagan_sin_avisos_ni_evento() {
        when(repositorio.aprobarSolicitud(any(), any(), any()))
                .thenThrow(new ConflictException(SesionErrorCode.SES_024))
                .thenThrow(new ConflictException(SesionErrorCode.SES_025))
                .thenThrow(new ForbiddenException("SEC_002", "Sin acceso al recurso solicitado."));

        assertThrows(ConflictException.class, () -> useCase.execute(COMANDO));
        assertThrows(ConflictException.class, () -> useCase.execute(COMANDO));
        assertThrows(ForbiddenException.class, () -> useCase.execute(COMANDO));
        verifyNoInteractions(notificaciones, realtime);
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new AprobarSolicitudSesionUseCaseImpl(null, repositorio, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new AprobarSolicitudSesionUseCaseImpl(sesiones, null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new AprobarSolicitudSesionUseCaseImpl(sesiones, repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new AprobarSolicitudSesionUseCaseImpl(sesiones, repositorio, notificaciones, null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private static DestinatarioSesionManual destinatario(final String tipo, final String nombre) {
        return new DestinatarioSesionManual(tipo, UUID.randomUUID(), nombre, nombre + "@uco.edu.co", "Calculo", "G01 de Calculo",
                "Maria Docente", INICIO, FIN);
    }

    private static SesionProcedenciaRepositoryProjection sesion(final String procedencia) {
        return new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "Sesion extra", 5, "COD5", 1, "01", "G01", INICIO, FIN, procedencia);
    }
}
