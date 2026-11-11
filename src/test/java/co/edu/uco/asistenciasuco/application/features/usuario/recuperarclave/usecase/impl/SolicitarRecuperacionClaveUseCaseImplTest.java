package co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.usecase.impl;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecuperacionClaveRepositoryPort.Solicitante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU177: pedir el enlace responde siempre lo mismo. Solo cuando la base de datos devuelve un solicitante se encola el
 * aviso, y el aviso nunca lleva el token ni el correo: el enlace lo emite el canal al entregar.
 */
class SolicitarRecuperacionClaveUseCaseImplTest {

    private static final String RESPUESTA = "Si el correo está registrado, recibirá un enlace para definir una contraseña nueva.";
    private static final UUID USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    private final RecuperacionClaveRepositoryPort repositorio = mock(RecuperacionClaveRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final MessageCatalogPort mensajes = mock(MessageCatalogPort.class);
    private final SolicitarRecuperacionClaveUseCaseImpl useCase =
            new SolicitarRecuperacionClaveUseCaseImpl(repositorio, notificaciones, mensajes);

    @BeforeEach
    void setUp() {
        when(mensajes.getUserMessage("RCL_001")).thenReturn(RESPUESTA);
    }

    @Test
    void con_un_solicitante_encola_el_aviso_para_ese_usuario_y_responde_el_mensaje_uniforme() {
        when(repositorio.consultarSolicitante("ana.perez@uco.edu.co")).thenReturn(Optional.of(new Solicitante(USUARIO, "ANA")));

        assertEquals(RESPUESTA, useCase.execute("  ana.perez@uco.edu.co  "));

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals(Notificacion.TIPO_RECUPERACION_CLAVE, aviso.getValue().tipo());
        assertEquals(1, aviso.getValue().destinatarios().size());
        assertEquals(USUARIO, aviso.getValue().destinatarios().getFirst().idUsuario());
        assertNull(aviso.getValue().destinatarios().getFirst().correo(), "El correo lo resuelve el canal, no el caso de uso.");
        assertEquals("Ana", aviso.getValue().datos().get("nombre"));
        assertFalse(aviso.getValue().datos().containsKey("enlace"), "El enlace no viaja en el outbox.");
    }

    @Test
    void sin_solicitante_responde_exactamente_igual_y_no_encola_nada() {
        when(repositorio.consultarSolicitante(anyString())).thenReturn(Optional.empty());

        assertEquals(RESPUESTA, useCase.execute("nadie@uco.edu.co"));

        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void correo_nulo_vacio_o_demasiado_largo_responde_igual_sin_consultar_la_base_de_datos() {
        assertEquals(RESPUESTA, useCase.execute(null));
        assertEquals(RESPUESTA, useCase.execute("   "));
        assertEquals(RESPUESTA, useCase.execute("a".repeat(101)));

        verify(repositorio, never()).consultarSolicitante(anyString());
        verify(notificaciones, never()).notificar(any());
    }

    @Test
    void sin_nombre_el_aviso_no_lleva_el_marcador_nombre() {
        when(repositorio.consultarSolicitante("ana.perez@uco.edu.co")).thenReturn(Optional.of(new Solicitante(USUARIO, null)));

        useCase.execute("ana.perez@uco.edu.co");

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertTrue(aviso.getValue().datos().isEmpty());
    }
}
