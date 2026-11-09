package co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.DestinatarioSesionManual;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/**
 * CULM-001 / DEC-064 (HU047): aviso inmediato de una sesion creada manualmente. Destinatarios (estudiantes y coordinadores),
 * outbox por tipo, evento {@code SESION_MANUAL_CREADA} y mejor esfuerzo: ningun fallo de aviso o evento revierte la sesion.
 */
class NotificarSesionManualUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTRA_SESION = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 13, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 10, 15, 0);
    private static final Comando COMANDO = new Comando(GRUPO, "Sesion extra", INICIO, FIN, DOCENTE);

    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final NotificarSesionManualUseCaseImpl useCase =
            new NotificarSesionManualUseCaseImpl(sesiones, repositorio, notificaciones, realtime);

    @Test
    void avisa_a_estudiantes_y_coordinadores_y_publica_el_evento_con_la_hora_utc_confirmada() {
        prepararSesion(SESION, 5, "UTC_V2");
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez", "ana@uco.edu.co"),
                destinatario(DestinatarioSesionManual.TIPO_COORDINADOR, "Carlos Coord", "carlos@uco.edu.co")));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, 1, 1, true), resultado);

        final ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisos.capture());
        final Notificacion alEstudiante = avisos.getAllValues().get(0);
        assertEquals(Notificacion.TIPO_SESION_MANUAL_CREADA_ESTUDIANTE, alEstudiante.tipo());
        assertEquals("Nueva sesión de clase: Calculo (10/11/2026)", alEstudiante.asunto());
        assertEquals("Hola Ana Perez. El docente Maria Docente programó una sesión adicional de Calculo del grupo G01 de Calculo "
                + "para el 10/11/2026 de 08:00 a 10:00.", alEstudiante.mensaje());
        assertEquals(List.of(new Notificacion.Destinatario("ESTUDIANTE", alEstudiante.destinatarios().getFirst().idUsuario(),
                "Ana Perez", "ana@uco.edu.co")), alEstudiante.destinatarios());
        assertEquals(Map.of("estudiante", "Ana Perez", "asignatura", "Calculo", "grupo", "G01 de Calculo", "fecha", "10/11/2026",
                "horaInicio", "08:00", "horaFin", "10:00", "docente", "Maria Docente", "idSesion", SESION.toString()),
                alEstudiante.datos());

        final Notificacion alCoordinador = avisos.getAllValues().get(1);
        assertEquals(Notificacion.TIPO_SESION_MANUAL_CREADA_COORDINADOR, alCoordinador.tipo());
        assertEquals("Sesión creada manualmente: Calculo (10/11/2026)", alCoordinador.asunto());
        assertEquals("COORDINADOR", alCoordinador.destinatarios().getFirst().rol());
        assertEquals("Carlos Coord", alCoordinador.datos().get("coordinador"));
        assertFalse(alCoordinador.datos().containsKey("estudiante"));
        assertTrue(alCoordinador.mensaje().contains("Si no corresponde, puede cancelarla desde su vista de sesiones."));

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("SESION_MANUAL_CREADA", evento.getValue().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "fechaHoraInicio", "2026-11-10T13:00:00Z",
                "docente", "Maria Docente"), evento.getValue().payload());
    }

    @Test
    void una_sesion_de_v1_publica_la_hora_local_sin_zona() {
        prepararSesion(SESION, 5, null);
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez", "ana@uco.edu.co")));

        useCase.execute(COMANDO);

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("2026-11-10T13:00", evento.getValue().payload().get("fechaHoraInicio"));
    }

    @Test
    void localiza_la_sesion_recien_creada_por_nombre_y_horas_y_elige_la_de_mayor_numero() {
        final SesionProcedenciaRepositoryProjection otraHora = proyeccion(UUID.randomUUID(), 9, "Sesion extra", INICIO.plusDays(1), FIN.plusDays(1), "UTC_V2");
        final SesionProcedenciaRepositoryProjection otroNombre = proyeccion(UUID.randomUUID(), 10, "Otra", INICIO, FIN, "UTC_V2");
        final SesionProcedenciaRepositoryProjection vieja = proyeccion(OTRA_SESION, 3, "Sesion extra", INICIO, FIN, "UTC_V2");
        final SesionProcedenciaRepositoryProjection nueva = proyeccion(SESION, 4, "  Sesion extra ", INICIO, FIN, "UTC_V2");
        when(sesiones.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(otraHora, otroNombre, vieja, nueva));
        when(repositorio.consultarDestinatarios(any(), any())).thenReturn(List.of());

        assertEquals(SESION, useCase.execute(COMANDO).sesionId());
        verify(repositorio).consultarDestinatarios(SESION, DOCENTE);
    }

    @Test
    void sin_la_sesion_en_el_grupo_no_avisa_ni_publica() {
        when(sesiones.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of());

        assertEquals(Resultado.sinSesion(), useCase.execute(COMANDO));
        assertNull(Resultado.sinSesion().sesionId());
        verifyNoInteractions(repositorio, notificaciones, realtime);
    }

    @Test
    void si_no_puede_leer_las_sesiones_no_falla_y_no_avisa() {
        when(sesiones.consultarSesionesPorGrupo(GRUPO)).thenThrow(new IllegalStateException("db"));

        assertEquals(Resultado.sinSesion(), useCase.execute(COMANDO));
        verifyNoInteractions(repositorio, notificaciones, realtime);
    }

    @Test
    void si_falla_la_consulta_de_destinatarios_igual_publica_el_evento() {
        prepararSesion(SESION, 5, "UTC_V2");
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenThrow(new IllegalStateException("db"));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, 0, 0, true), resultado);
        verify(notificaciones, never()).notificar(any());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("", evento.getValue().payload().get("docente"));
    }

    @Test
    void un_aviso_fallido_no_detiene_los_demas_ni_se_cuenta() {
        prepararSesion(SESION, 5, "UTC_V2");
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez", "ana@uco.edu.co"),
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Luis Gomez", "luis@uco.edu.co"),
                destinatario(DestinatarioSesionManual.TIPO_COORDINADOR, "Carlos Coord", "carlos@uco.edu.co")));
        doThrow(new IllegalStateException("smtp")).when(notificaciones)
                .notificar(org.mockito.ArgumentMatchers.argThat(aviso -> aviso.destinatarios().getFirst().nombre().equals("Ana Perez")));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, 1, 1, true), resultado);
        verify(notificaciones, times(3)).notificar(any());
    }

    @Test
    void un_fallo_del_publicador_no_cambia_el_resultado_de_los_avisos() {
        prepararSesion(SESION, 5, "UTC_V2");
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario(DestinatarioSesionManual.TIPO_ESTUDIANTE, "Ana Perez", "ana@uco.edu.co")));
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());

        assertEquals(new Resultado(SESION, 1, 0, false), useCase.execute(COMANDO));
    }

    @Test
    void tolera_destinatarios_con_datos_nulos_y_tipos_desconocidos() {
        prepararSesion(SESION, 5, "UTC_V2");
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                new DestinatarioSesionManual(DestinatarioSesionManual.TIPO_ESTUDIANTE, UUID.randomUUID(), null, null, null, null, null, null, null),
                new DestinatarioSesionManual("OTRO", UUID.randomUUID(), "Otro", "otro@uco.edu.co", "Calculo", "G01", "Docente", INICIO, FIN)));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, 1, 0, true), resultado);
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals("Nueva sesión de clase:  ()", aviso.getValue().asunto());
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new NotificarSesionManualUseCaseImpl(null, repositorio, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new NotificarSesionManualUseCaseImpl(sesiones, null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new NotificarSesionManualUseCaseImpl(sesiones, repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new NotificarSesionManualUseCaseImpl(sesiones, repositorio, notificaciones, null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private void prepararSesion(final UUID id, final int numero, final String procedencia) {
        when(sesiones.consultarSesionesPorGrupo(GRUPO))
                .thenReturn(List.of(proyeccion(id, numero, "Sesion extra", INICIO, FIN, procedencia)));
    }

    private static SesionProcedenciaRepositoryProjection proyeccion(final UUID id, final int numero, final String nombre,
                                                                    final LocalDateTime inicio, final LocalDateTime fin,
                                                                    final String procedencia) {
        return new SesionProcedenciaRepositoryProjection(id, GRUPO, nombre, numero, "COD" + numero, 1, "01", "G01", inicio, fin, procedencia);
    }

    private static DestinatarioSesionManual destinatario(final String tipo, final String nombre, final String correo) {
        return new DestinatarioSesionManual(tipo, UUID.randomUUID(), nombre, correo, "Calculo", "G01 de Calculo", "Maria Docente", INICIO, FIN);
    }
}
