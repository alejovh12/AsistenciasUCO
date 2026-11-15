package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort.FuenteNoDisponibleException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogSourcePort.MensajeExterno;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.exception.MessageClientResponseException;
import co.edu.uco.messageucolab.client.api.exception.MessageClientTransportException;
import co.edu.uco.messageucolab.client.api.model.MessageCollection;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** INT-003: el codigo canonico sale del titulo y solo vale si su traduccion da el codigo del mensaje. */
class MessageUcoLabMessageSourceAdapterTest {

    private final MessageReader reader = mock(MessageReader.class);
    private final MessageUcoLabMessageSourceAdapter adapter = new MessageUcoLabMessageSourceAdapter(reader,
            new MessageUcoLabCodeMapper("MSG_", Map.of("ERR_CORRELACION_REQUERIDA", "ERR001")), Duration.ofSeconds(1));

    private static MessageDetails mensaje(final String code, final String title, final String content) {
        return new MessageDetails(code, "es", "es", title, content, "Funcional", "Error", "Mensajes institucionales",
                Instant.EPOCH, Instant.EPOCH);
    }

    @Test
    void obtiene_el_canonico_del_titulo() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(mensaje("MSG_VAL001", "VAL_001 - El campo…", "El campo {} es obligatorio.")));

        assertEquals(Optional.of(new MensajeExterno("VAL_001", "El campo {} es obligatorio.")), adapter.obtener(" MSG_VAL001 "));
    }

    @Test
    void acepta_titulos_que_son_solo_el_canonico_y_codigos_con_alias() {
        when(reader.getDetails("MSG_ERR001")).thenReturn(Mono.just(mensaje("msg_err001", "ERR_CORRELACION_REQUERIDA", "Falta la correlacion.")));

        assertEquals(Optional.of(new MensajeExterno("ERR_CORRELACION_REQUERIDA", "Falta la correlacion.")), adapter.obtener("MSG_ERR001"));
    }

    @Test
    void descarta_titulos_que_no_corresponden_al_codigo() {
        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.just(mensaje("MSG_VAL001", "VAL_002 - Otro", "Texto")));
        when(reader.getDetails("MSG_LARGO")).thenReturn(Mono.just(mensaje("MSG_LARGO", "CODIGO_MUY_LARGO_SIN_ALIAS - X", "Texto")));

        assertTrue(adapter.obtener("MSG_VAL001").isEmpty());
        assertTrue(adapter.obtener("MSG_LARGO").isEmpty());
    }

    @Test
    void descarta_mensajes_incompletos() {
        assertTrue(adapter.traducir(null).isEmpty());
        assertTrue(adapter.traducir(mensaje(" ", "VAL_001", "x")).isEmpty());
        assertTrue(adapter.traducir(mensaje("MSG_VAL001", null, "x")).isEmpty());
        assertTrue(adapter.traducir(mensaje("MSG_VAL001", "VAL_001", " ")).isEmpty());
        assertTrue(adapter.traducir(mensaje(null, "VAL_001", "x")).isEmpty());
        assertTrue(adapter.traducir(mensaje("MSG_VAL001", "VAL_001", null)).isEmpty());
        assertTrue(adapter.traducir(mensaje("MSG_VAL001", "", "x")).isEmpty());
    }

    @Test
    void codigo_vacio_no_consulta_y_un_mensaje_inexistente_devuelve_vacio() {
        assertTrue(adapter.obtener(null).isEmpty());
        assertTrue(adapter.obtener(" ").isEmpty());
        verifyNoInteractions(reader);

        when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.error(new MessageClientResponseException(404, null)));
        assertTrue(adapter.obtener("MSG_VAL001").isEmpty());
        when(reader.getDetails("MSG_VAL002")).thenReturn(Mono.empty());
        assertTrue(adapter.obtener("MSG_VAL002").isEmpty());
    }

    /**
     * Regresion de la carga de 358 mensajes (2026-11-15): el limite de Kong (120/min) respondio 429 y el adaptador lo
     * tomaba como "mensaje inexistente": el aviso se confirmaba sin copiar el texto. Un fallo transitorio debe propagarse
     * para que el aviso vuelva a Pulsar.
     */
    @Test
    void un_fallo_transitorio_no_se_confunde_con_un_mensaje_inexistente() {
        for (final int estado : new int[] {429, 401, 500, 503}) {
            when(reader.getDetails("MSG_VAL001")).thenReturn(Mono.error(new MessageClientResponseException(estado, null)));
            final FuenteNoDisponibleException error =
                    assertThrows(FuenteNoDisponibleException.class, () -> adapter.obtener("MSG_VAL001"));
            assertTrue(error.getMessage().contains(String.valueOf(estado)), error::getMessage);
        }
        when(reader.getDetails("MSG_VAL002")).thenReturn(Mono.error(new MessageClientTransportException(new IllegalStateException("caido"))));
        assertThrows(FuenteNoDisponibleException.class, () -> adapter.obtener("MSG_VAL002"));
        when(reader.getDetails("MSG_VAL003")).thenReturn(Mono.error(new IllegalStateException("inesperado")));
        assertThrows(FuenteNoDisponibleException.class, () -> adapter.obtener("MSG_VAL003"));
    }

    @Test
    void listar_devuelve_solo_los_de_asistencias() {
        when(reader.getAll()).thenReturn(Mono.just(new MessageCollection("es", 3, Arrays.asList(
                mensaje("MSG_VAL001", "VAL_001 - x", "Uno"),
                mensaje("MSG_ZZ", "Mensaje de otra aplicacion", "Dos"),
                mensaje("MSG_ERR001", "ERR_CORRELACION_REQUERIDA", "Tres")))));

        assertEquals(List.of(new MensajeExterno("VAL_001", "Uno"), new MensajeExterno("ERR_CORRELACION_REQUERIDA", "Tres")),
                adapter.listar());
    }

    @Test
    void listar_sin_respuesta_devuelve_lista_vacia_y_un_fallo_se_propaga() {
        when(reader.getAll()).thenReturn(Mono.empty());
        assertEquals(List.of(), adapter.listar());
        when(reader.getAll()).thenReturn(Mono.error(new MessageClientResponseException(429, null)));
        assertThrows(FuenteNoDisponibleException.class, adapter::listar);
        when(reader.getAll()).thenReturn(Mono.error(new IllegalStateException("caido")));
        assertThrows(FuenteNoDisponibleException.class, adapter::listar);
    }

    @Test
    void el_listado_espera_al_menos_treinta_segundos_y_respeta_un_timeout_mayor() {
        final MessageUcoLabMessageSourceAdapter lento = new MessageUcoLabMessageSourceAdapter(reader,
                new MessageUcoLabCodeMapper("MSG_", Map.of()), Duration.ofMinutes(2));
        when(reader.getAll()).thenReturn(Mono.just(new MessageCollection("es", 0, List.of())));
        assertEquals(List.of(), lento.listar());
    }

    @Test
    void dependencias_obligatorias() {
        final MessageUcoLabCodeMapper mapper = new MessageUcoLabCodeMapper("MSG_", Map.of());
        final Duration unSegundo = Duration.ofSeconds(1);
        assertThrows(NullPointerException.class, () -> new MessageUcoLabMessageSourceAdapter(null, mapper, unSegundo));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabMessageSourceAdapter(reader, null, unSegundo));
        assertThrows(NullPointerException.class, () -> new MessageUcoLabMessageSourceAdapter(reader, mapper, null));
    }
}
