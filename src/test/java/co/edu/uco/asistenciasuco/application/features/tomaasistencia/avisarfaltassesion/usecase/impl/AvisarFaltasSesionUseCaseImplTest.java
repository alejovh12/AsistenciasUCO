package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort.FaltaSesionAviso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoFaltaSesionRepositoryPort.SesionPendienteAviso;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ATA D1 (HU175): aviso de falta por sesion, un correo por estudiante, mejor esfuerzo y marca solo cuando la sesion se pudo avisar. */
class AvisarFaltasSesionUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION_2 = UUID.fromString("11111111-1111-1111-1111-222222222222");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID USUARIO_1 = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID USUARIO_2 = UUID.fromString("44444444-4444-4444-4444-444444444442");
    // 2026-11-17 13:00Z = 08:00 en Bogota (UTC-5); la sesion dura 2 h
    private static final Instant INICIO = Instant.parse("2026-11-17T13:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-17T15:00:00Z");
    // medianoche local (05:00Z) del 23/11: el ultimo dia para reclamar es el 22/11
    private static final Instant LIMITE = Instant.parse("2026-11-23T05:00:00Z");

    private final AvisoFaltaSesionRepositoryPort repositorio = mock(AvisoFaltaSesionRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);

    private AvisarFaltasSesionUseCaseImpl caso(final String url) {
        return new AvisarFaltasSesionUseCaseImpl(repositorio, notificaciones, url);
    }

    private static SesionPendienteAviso pendiente(final UUID sesion) {
        return new SesionPendienteAviso(sesion, GRUPO, FIN);
    }

    private static FaltaSesionAviso falta(final UUID sesion, final UUID usuario, final String nombre, final int toma, final String puntoControl,
                                          final int enFalta, final int total) {
        return new FaltaSesionAviso(sesion, GRUPO, ASIGNATURA, "Ingeniería de Software", "G01 de Ingeniería de Software", INICIO, FIN, usuario,
                nombre, nombre.toLowerCase().replace(' ', '.') + "@uco.test", toma, Instant.parse(puntoControl), total, enFalta, 40, LIMITE);
    }

    @Test
    void falta_general_cuando_quedo_en_sjc_en_todas_las_tomas() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(
                falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 2, 2),
                falta(SESION, USUARIO_1, "Ana Gomez", 2, "2026-11-17T14:00:00Z", 2, 2)));

        assertEquals(1, caso("https://asistencia.uco.test").execute());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(captor.capture());
        final Notificacion aviso = captor.getValue();
        assertEquals(Notificacion.TIPO_FALTA_ASISTENCIA_SESION_ESTUDIANTE, aviso.tipo());
        assertEquals("Registro de inasistencia: Ingeniería de Software (17/11/2026)", aviso.asunto());
        assertEquals(1, aviso.destinatarios().size());
        final Notificacion.Destinatario destinatario = aviso.destinatarios().getFirst();
        assertEquals("ESTUDIANTE", destinatario.rol());
        assertEquals(USUARIO_1, destinatario.idUsuario());
        assertEquals("Ana Gomez", destinatario.nombre());
        assertEquals("ana.gomez@uco.test", destinatario.correo());
        assertEquals(AvisarFaltasSesionUseCaseImpl.FALTA_GENERAL, aviso.datos().get("detalleFaltas"));
        assertEquals("08:00", aviso.datos().get("horaInicio"));
        assertEquals("10:00", aviso.datos().get("horaFin"));
        assertEquals("22/11/2026", aviso.datos().get("fechaLimiteReclamo"));
        assertEquals("https://asistencia.uco.test/app/estudiante/mis-materias/materias?materia=" + ASIGNATURA + "&grupo=" + GRUPO,
                aviso.datos().get("enlaceReclamo"));
        assertEquals(SESION.toString(), aviso.datos().get("idSesion"));
        assertTrue(aviso.mensaje().contains("falta general"));
        assertTrue(aviso.mensaje().contains("hasta el 22/11/2026 desde https://asistencia.uco.test/app/estudiante"));
        verify(repositorio).registrarAviso(SESION, 1);
    }

    @Test
    void faltas_parciales_listan_las_tomas_con_su_hora_institucional_en_orden() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(
                falta(SESION, USUARIO_1, "Ana Gomez", 3, "2026-11-17T14:40:00Z", 2, 3),
                falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:15:00Z", 2, 3)));

        assertEquals(1, caso("https://asistencia.uco.test/").execute());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(captor.capture());
        assertEquals("toma 1 (08:15), toma 3 (09:40)", captor.getValue().datos().get("detalleFaltas"));
        // la URL con barra final no duplica la barra
        assertTrue(captor.getValue().datos().get("enlaceReclamo").startsWith("https://asistencia.uco.test/app/estudiante/"));
        assertTrue(captor.getValue().mensaje().contains("quedó registrada inasistencia: toma 1 (08:15), toma 3 (09:40)."));
    }

    @Test
    void un_correo_por_estudiante_aunque_tenga_varias_tomas_en_falta() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(
                falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 2),
                falta(SESION, USUARIO_2, "Beto Ruiz", 1, "2026-11-17T13:00:00Z", 2, 2),
                falta(SESION, USUARIO_2, "Beto Ruiz", 2, "2026-11-17T14:00:00Z", 2, 2)));

        assertEquals(1, caso("").execute());

        verify(notificaciones, times(2)).notificar(any());
        verify(repositorio).registrarAviso(SESION, 2);
    }

    @Test
    void sin_url_de_plataforma_el_aviso_nombra_la_plataforma_sin_enlace() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 1)));

        caso(null).execute();

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(captor.capture());
        assertEquals(AvisarFaltasSesionUseCaseImpl.PLATAFORMA_SIN_ENLACE, captor.getValue().datos().get("enlaceReclamo"));
    }

    @Test
    void sin_estudiantes_con_falta_la_sesion_se_marca_con_total_cero_sin_enviar() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of());

        assertEquals(1, caso("").execute());

        verify(notificaciones, never()).notificar(any());
        verify(repositorio).registrarAviso(SESION, 0);
    }

    @Test
    void sin_sesiones_pendientes_no_hace_nada() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of());

        assertEquals(0, caso("").execute());

        verify(repositorio, never()).consultarFaltas(any());
        verify(repositorio, never()).registrarAviso(any(), anyInt());
    }

    @Test
    void un_fallo_de_envio_continua_con_los_demas_y_marca_los_avisados() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(
                falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 1),
                falta(SESION, USUARIO_2, "Beto Ruiz", 1, "2026-11-17T13:00:00Z", 1, 1)));
        doThrow(new IllegalStateException("outbox")).doNothing().when(notificaciones).notificar(any());

        assertEquals(1, caso("").execute());

        verify(notificaciones, times(2)).notificar(any());
        verify(repositorio).registrarAviso(SESION, 1);
    }

    @Test
    void si_todos_los_envios_fallan_la_sesion_no_se_marca() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 1)));
        doThrow(new IllegalStateException("outbox")).when(notificaciones).notificar(any());

        assertEquals(0, caso("").execute());

        verify(repositorio, never()).registrarAviso(any(), anyInt());
    }

    @Test
    void un_fallo_al_consultar_las_faltas_no_marca_la_sesion_y_sigue_con_la_siguiente() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION), pendiente(SESION_2)));
        when(repositorio.consultarFaltas(SESION)).thenThrow(new IllegalStateException("db"));
        when(repositorio.consultarFaltas(SESION_2)).thenReturn(List.of(falta(SESION_2, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 1)));

        assertEquals(1, caso("").execute());

        verify(repositorio, never()).registrarAviso(SESION, 0);
        verify(repositorio).registrarAviso(SESION_2, 1);
    }

    @Test
    void un_fallo_al_registrar_la_marca_no_cuenta_la_sesion_como_avisada() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(falta(SESION, USUARIO_1, "Ana Gomez", 1, "2026-11-17T13:00:00Z", 1, 1)));
        doThrow(new IllegalStateException("db")).when(repositorio).registrarAviso(SESION, 1);

        assertEquals(0, caso("").execute());
    }

    @Test
    void sin_fecha_limite_el_marcador_va_vacio() {
        final FaltaSesionAviso sinLimite = new FaltaSesionAviso(SESION, GRUPO, ASIGNATURA, "Ingeniería de Software", "G01", INICIO, FIN, USUARIO_1,
                "Ana Gomez", "ana@uco.test", 1, INICIO, 1, 1, 40, null);
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarFaltas(SESION)).thenReturn(List.of(sinLimite));

        caso("").execute();

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(captor.capture());
        assertEquals("", captor.getValue().datos().get("fechaLimiteReclamo"));
    }

    @Test
    void valida_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new AvisarFaltasSesionUseCaseImpl(null, notificaciones, ""));
        assertThrows(NullPointerException.class, () -> new AvisarFaltasSesionUseCaseImpl(repositorio, null, ""));
    }
}
