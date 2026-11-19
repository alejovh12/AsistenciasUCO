package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarsesionessintoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.DestinatarioAvisoSinToma;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AvisoSesionSinTomaRepositoryPort.SesionSinTomaPendiente;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTOMA-001: aviso a la coordinacion de la sesion sin toma; un correo por coordinador, mejor esfuerzo y marca solo si el aviso salio. */
class AvisarSesionesSinTomaUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESION_2 = UUID.fromString("11111111-1111-1111-1111-222222222222");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ASIGNATURA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID COORDINADOR_1 = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final UUID COORDINADOR_2 = UUID.fromString("44444444-4444-4444-4444-444444444442");
    // 2026-11-17 13:00Z = 08:00 en Bogota (UTC-5); la sesion dura 2 h
    private static final Instant INICIO = Instant.parse("2026-11-17T13:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-17T15:00:00Z");

    private final AvisoSesionSinTomaRepositoryPort repositorio = mock(AvisoSesionSinTomaRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);

    private AvisarSesionesSinTomaUseCaseImpl caso(final String url) {
        return new AvisarSesionesSinTomaUseCaseImpl(repositorio, notificaciones, url);
    }

    private static SesionSinTomaPendiente pendiente(final UUID sesion) {
        return new SesionSinTomaPendiente(sesion, GRUPO, FIN);
    }

    private static DestinatarioAvisoSinToma destinatario(final UUID sesion, final UUID usuario, final String nombre) {
        return new DestinatarioAvisoSinToma(sesion, GRUPO, ASIGNATURA, "Ingeniería de Software", "G01 de Ingeniería de Software", "María Gómez",
                INICIO, FIN, usuario, nombre, nombre.toLowerCase().replace(' ', '.') + "@uco.test");
    }

    @Test
    void envia_un_correo_por_coordinador_con_los_datos_de_la_sesion_y_marca_el_total() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(
                destinatario(SESION, COORDINADOR_1, "Laura Martinez"), destinatario(SESION, COORDINADOR_2, "Pedro Ruiz")));

        assertEquals(1, caso("https://asistencia.uco.test").execute());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(captor.capture());
        final Notificacion primero = captor.getAllValues().getFirst();
        assertEquals(Notificacion.TIPO_SESION_SIN_TOMA_COORDINADOR, primero.tipo());
        assertEquals("Sesión sin toma de asistencia: Ingeniería de Software (17/11/2026)", primero.asunto());
        assertEquals(1, primero.destinatarios().size());
        final Notificacion.Destinatario destinatario = primero.destinatarios().getFirst();
        assertEquals("COORDINADOR", destinatario.rol());
        assertEquals(COORDINADOR_1, destinatario.idUsuario());
        assertEquals("Laura Martinez", destinatario.nombre());
        assertEquals("laura.martinez@uco.test", destinatario.correo());
        assertEquals("Laura Martinez", primero.datos().get("coordinador"));
        assertEquals("María Gómez", primero.datos().get("docente"));
        assertEquals("Ingeniería de Software", primero.datos().get("asignatura"));
        assertEquals("G01 de Ingeniería de Software", primero.datos().get("grupo"));
        assertEquals("17/11/2026", primero.datos().get("fecha"));
        assertEquals("08:00", primero.datos().get("horaInicio"));
        assertEquals("10:00", primero.datos().get("horaFin"));
        assertEquals("https://asistencia.uco.test/app/coordinador/asistencia", primero.datos().get("enlaceSesiones"));
        assertEquals(SESION.toString(), primero.datos().get("idSesion"));
        assertEquals("Hola Laura Martinez. El docente María Gómez no tomó asistencia en la sesión de Ingeniería de Software del grupo "
                + "G01 de Ingeniería de Software del 17/11/2026 de 08:00 a 10:00. Este aviso es solo informativo; puede revisar el grupo en "
                + "https://asistencia.uco.test/app/coordinador/asistencia.", primero.mensaje());
        assertEquals(COORDINADOR_2, captor.getAllValues().get(1).destinatarios().getFirst().idUsuario());
        verify(repositorio).registrarAviso(SESION, 2);
    }

    @Test
    void la_marca_se_escribe_despues_de_encolar_los_correos() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(destinatario(SESION, COORDINADOR_1, "Laura Martinez")));

        caso("").execute();

        final var orden = inOrder(notificaciones, repositorio);
        orden.verify(notificaciones).notificar(any());
        orden.verify(repositorio).registrarAviso(SESION, 1);
    }

    @Test
    void la_url_con_barra_final_no_duplica_la_barra_y_sin_url_se_nombra_la_plataforma() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(destinatario(SESION, COORDINADOR_1, "Laura Martinez")));

        caso("https://asistencia.uco.test/").execute();
        caso(null).execute();

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(captor.capture());
        assertEquals("https://asistencia.uco.test/app/coordinador/asistencia", captor.getAllValues().get(0).datos().get("enlaceSesiones"));
        assertEquals(AvisarSesionesSinTomaUseCaseImpl.PLATAFORMA_SIN_ENLACE, captor.getAllValues().get(1).datos().get("enlaceSesiones"));
        assertTrue(captor.getAllValues().get(1).mensaje().endsWith("puede revisar el grupo en la plataforma de asistencia."));
    }

    @Test
    void sin_coordinadores_la_sesion_se_marca_con_cero_para_no_consultarla_otra_vez() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of());

        assertEquals(1, caso("").execute());

        verify(notificaciones, never()).notificar(any());
        verify(repositorio).registrarAviso(SESION, 0);
    }

    @Test
    void un_fallo_con_un_coordinador_no_impide_avisar_a_los_demas_y_la_sesion_se_marca_con_los_avisados() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(
                destinatario(SESION, COORDINADOR_1, "Laura Martinez"), destinatario(SESION, COORDINADOR_2, "Pedro Ruiz")));
        doThrow(new IllegalStateException("smtp")).when(notificaciones)
                .notificar(argThat(n -> n != null && COORDINADOR_1.equals(n.destinatarios().getFirst().idUsuario())));

        assertEquals(1, caso("").execute());

        verify(notificaciones, times(2)).notificar(any());
        verify(repositorio).registrarAviso(SESION, 1);
    }

    @Test
    void si_ningun_correo_pudo_encolarse_la_sesion_no_se_marca_y_se_reintenta_en_la_siguiente_pasada() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(destinatario(SESION, COORDINADOR_1, "Laura Martinez")));
        doThrow(new IllegalStateException("outbox")).when(notificaciones).notificar(any());

        assertEquals(0, caso("").execute());

        verify(repositorio, never()).registrarAviso(any(), anyInt());
    }

    @Test
    void un_fallo_al_consultar_los_destinatarios_no_marca_la_sesion_ni_impide_las_demas() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION), pendiente(SESION_2)));
        when(repositorio.consultarDestinatarios(SESION)).thenThrow(new IllegalStateException("db"));
        when(repositorio.consultarDestinatarios(SESION_2)).thenReturn(List.of(destinatario(SESION_2, COORDINADOR_1, "Laura Martinez")));

        assertEquals(1, caso("").execute());

        verify(repositorio, never()).registrarAviso(SESION, 1);
        verify(repositorio).registrarAviso(SESION_2, 1);
    }

    @Test
    void un_fallo_al_registrar_la_marca_no_se_propaga_y_no_cuenta_la_sesion() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(destinatario(SESION, COORDINADOR_1, "Laura Martinez")));
        doThrow(new IllegalStateException("db")).when(repositorio).registrarAviso(SESION, 1);

        assertEquals(0, caso("").execute());
    }

    @Test
    void sin_sesiones_pendientes_no_hace_nada() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of());

        assertEquals(0, caso("").execute());

        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void los_datos_ausentes_se_muestran_vacios_y_las_dependencias_son_obligatorias() {
        when(repositorio.consultarSesionesPendientes()).thenReturn(List.of(pendiente(SESION)));
        when(repositorio.consultarDestinatarios(SESION)).thenReturn(List.of(new DestinatarioAvisoSinToma(
                SESION, GRUPO, ASIGNATURA, null, null, null, null, null, COORDINADOR_1, null, "c@uco.test")));

        assertEquals(1, caso("").execute());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(captor.capture());
        assertEquals("", captor.getValue().datos().get("fecha"));
        assertEquals("", captor.getValue().datos().get("docente"));

        assertThrows(NullPointerException.class, () -> new AvisarSesionesSinTomaUseCaseImpl(null, notificaciones, ""));
        assertThrows(NullPointerException.class, () -> new AvisarSesionesSinTomaUseCaseImpl(repositorio, null, ""));
    }
}
