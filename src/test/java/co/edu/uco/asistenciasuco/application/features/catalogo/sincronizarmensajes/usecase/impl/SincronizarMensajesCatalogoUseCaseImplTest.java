package co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.AvisoCambioMensaje;
import co.edu.uco.asistenciasuco.application.features.catalogo.sincronizarmensajes.primaryports.SincronizarMensajesCatalogoInputPort.ResumenSincronizacion;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.CatalogInvalidationPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort.MensajeExterno;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MensajeCatalogoRepositoryPort.ResultadoSincronizacion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** INT-003: avisos de MessageUcoLab -> cache, BD (usp_sincronizar_mensaje_usuario) y tiempo real. */
class SincronizarMensajesCatalogoUseCaseImplTest {

    private final MessageCatalogSourcePort fuente = mock(MessageCatalogSourcePort.class);
    private final MensajeCatalogoRepositoryPort repositorio = mock(MensajeCatalogoRepositoryPort.class);
    private final CatalogInvalidationPort invalidacion = mock(CatalogInvalidationPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final SincronizarMensajesCatalogoUseCaseImpl useCase =
            new SincronizarMensajesCatalogoUseCaseImpl(fuente, repositorio, invalidacion, realtime);

    @Test
    void mensaje_actualizado_invalida_su_cache_guarda_en_bd_y_avisa_en_tiempo_real() {
        when(fuente.obtener("MSG_VAL001")).thenReturn(Optional.of(new MensajeExterno("VAL_001", "Texto nuevo")));
        when(repositorio.sincronizarMensajeUsuario("VAL_001", "Texto nuevo")).thenReturn(ResultadoSincronizacion.ACTUALIZADO);

        final ResumenSincronizacion resumen = useCase.procesarAviso(new AvisoCambioMensaje("e1", "MessageUpdated", " MSG_VAL001 "));

        assertEquals(new ResumenSincronizacion(1, 1, 0, 0, 0), resumen);
        verify(invalidacion).invalidateMessage("VAL_001");
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("MESSAGE_UPDATED", evento.getValue().type());
        assertEquals("VAL_001", evento.getValue().payload().get("code"));
        assertEquals("MESSAGEUCOLAB", evento.getValue().payload().get("source"));
    }

    @Test
    void mensaje_creado_con_el_mismo_texto_no_publica_en_tiempo_real() {
        when(fuente.obtener("MSG_VAL001")).thenReturn(Optional.of(new MensajeExterno("VAL_001", "Igual")));
        when(repositorio.sincronizarMensajeUsuario("VAL_001", "Igual")).thenReturn(ResultadoSincronizacion.SIN_CAMBIOS);

        assertEquals(new ResumenSincronizacion(1, 0, 1, 0, 0),
                useCase.procesarAviso(new AvisoCambioMensaje(null, "MessageCreated", "MSG_VAL001")));
        verify(realtime, never()).publish(any());
    }

    @Test
    void codigo_que_la_bd_no_tiene_se_cuenta_como_no_existente() {
        when(fuente.obtener("MSG_UI001")).thenReturn(Optional.of(new MensajeExterno("UI_001", "Texto UI")));
        when(repositorio.sincronizarMensajeUsuario("UI_001", "Texto UI")).thenReturn(ResultadoSincronizacion.NO_EXISTE);

        assertEquals(new ResumenSincronizacion(1, 0, 0, 1, 0),
                useCase.procesarAviso(new AvisoCambioMensaje("e2", "MessageUpdated", "MSG_UI001")));
        verify(realtime, never()).publish(any());
    }

    @Test
    void aviso_repetido_se_ignora() {
        when(fuente.obtener("MSG_VAL001")).thenReturn(Optional.of(new MensajeExterno("VAL_001", "T")));
        when(repositorio.sincronizarMensajeUsuario("VAL_001", "T")).thenReturn(ResultadoSincronizacion.SIN_CAMBIOS);

        useCase.procesarAviso(new AvisoCambioMensaje("e3", "MessageUpdated", "MSG_VAL001"));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(new AvisoCambioMensaje(" e3 ", "MessageUpdated", "MSG_VAL001")));

        verify(fuente, times(1)).obtener("MSG_VAL001");
    }

    @Test
    void aviso_que_fallo_en_la_bd_no_queda_como_procesado_para_que_el_broker_lo_reintente() {
        when(fuente.obtener("MSG_VAL001")).thenReturn(Optional.of(new MensajeExterno("VAL_001", "T")));
        when(repositorio.sincronizarMensajeUsuario("VAL_001", "T"))
                .thenThrow(new IllegalStateException("BD caida"))
                .thenReturn(ResultadoSincronizacion.ACTUALIZADO);
        final AvisoCambioMensaje aviso = new AvisoCambioMensaje("e4", "MessageUpdated", "MSG_VAL001");

        assertThrows(IllegalStateException.class, () -> useCase.procesarAviso(aviso));
        assertEquals(new ResumenSincronizacion(1, 1, 0, 0, 0), useCase.procesarAviso(aviso));
    }

    @Test
    void mensaje_eliminado_solo_vence_la_cache_y_conserva_la_bd() {
        assertEquals(ResumenSincronizacion.vacio(),
                useCase.procesarAviso(new AvisoCambioMensaje("e5", "MessageDeleted", "MSG_VAL001")));
        verify(invalidacion).invalidateAll();
        verifyNoInteractions(repositorio, fuente, realtime);
    }

    @Test
    void mensaje_no_legible_o_ajeno_vence_toda_la_cache() {
        when(fuente.obtener("MSG_OTRO")).thenReturn(Optional.empty());

        assertEquals(ResumenSincronizacion.vacio(),
                useCase.procesarAviso(new AvisoCambioMensaje("e6", "MessageUpdated", "MSG_OTRO")));
        verify(invalidacion).invalidateAll();
        verifyNoInteractions(repositorio);
    }

    @Test
    void cambio_colectivo_o_aviso_sin_codigo_reconcilia_todo() {
        when(fuente.listar()).thenReturn(List.of());

        useCase.procesarAviso(new AvisoCambioMensaje("e7", "MessagesChanged", ""));
        useCase.procesarAviso(new AvisoCambioMensaje("e8", "MessageUpdated", "  "));

        verify(fuente, times(2)).listar();
        verify(invalidacion, times(2)).invalidateAll();
    }

    @Test
    void aviso_nulo_sin_tipo_o_de_tipo_desconocido_no_hace_nada() {
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(null));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(new AvisoCambioMensaje("e9", " ", "MSG_X")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(new AvisoCambioMensaje("e10", null, "MSG_X")));
        assertEquals(ResumenSincronizacion.vacio(), useCase.procesarAviso(new AvisoCambioMensaje("e11", "Otro", "MSG_X")));
        verifyNoInteractions(fuente, repositorio, invalidacion, realtime);
    }

    @Test
    void reconciliacion_cuenta_cada_resultado_y_sigue_aunque_un_mensaje_falle() {
        when(fuente.listar()).thenReturn(List.of(
                new MensajeExterno("A_1", "a"), new MensajeExterno("B_1", "b"),
                new MensajeExterno("C_1", "c"), new MensajeExterno("D_1", "d")));
        when(repositorio.sincronizarMensajeUsuario("A_1", "a")).thenReturn(ResultadoSincronizacion.ACTUALIZADO);
        when(repositorio.sincronizarMensajeUsuario("B_1", "b")).thenReturn(ResultadoSincronizacion.SIN_CAMBIOS);
        when(repositorio.sincronizarMensajeUsuario("C_1", "c")).thenReturn(ResultadoSincronizacion.NO_EXISTE);
        when(repositorio.sincronizarMensajeUsuario("D_1", "d")).thenThrow(new IllegalStateException("fallo"));

        assertEquals(new ResumenSincronizacion(4, 1, 1, 1, 1), useCase.reconciliarTodo());
        verify(invalidacion).invalidateAll();
        verify(realtime, times(1)).publish(any());
        verify(invalidacion, times(4)).invalidateMessage(anyString());
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new SincronizarMensajesCatalogoUseCaseImpl(null, repositorio, invalidacion, realtime));
        assertThrows(NullPointerException.class, () -> new SincronizarMensajesCatalogoUseCaseImpl(fuente, null, invalidacion, realtime));
        assertThrows(NullPointerException.class, () -> new SincronizarMensajesCatalogoUseCaseImpl(fuente, repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new SincronizarMensajesCatalogoUseCaseImpl(fuente, repositorio, invalidacion, null));
    }

    @Test
    void recuerda_un_numero_acotado_de_avisos() {
        when(fuente.obtener(anyString())).thenReturn(Optional.empty());
        for (int i = 0; i <= SincronizarMensajesCatalogoUseCaseImpl.MAX_AVISOS_RECORDADOS; i++) {
            useCase.procesarAviso(new AvisoCambioMensaje("id-" + i, "MessageUpdated", "MSG_X"));
        }
        // El primero salio de la memoria: se vuelve a procesar.
        useCase.procesarAviso(new AvisoCambioMensaje("id-0", "MessageUpdated", "MSG_X"));
        verify(fuente, times(SincronizarMensajesCatalogoUseCaseImpl.MAX_AVISOS_RECORDADOS + 2)).obtener("MSG_X");
    }
}
