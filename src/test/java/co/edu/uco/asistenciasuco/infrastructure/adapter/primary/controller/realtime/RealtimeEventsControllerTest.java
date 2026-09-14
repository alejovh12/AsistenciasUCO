package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RealtimeEventsControllerTest {

    @Test
    void subscriberReceivesConnectionChangeAndHeartbeatEvents() throws Exception {
        final RealtimeEventHub hub = new RealtimeEventHub();
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RealtimeEventsController(hub)).build();

        final MvcResult stream = mvc.perform(get("/api/v1/realtime/stream").param("clientId", "cliente-1"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(stream.getRequest().isAsyncStarted());
        assertEquals(1, hub.getActiveSubscribersCount());

        mvc.perform(post("/api/v1/realtime/emit")
                        .contentType("application/json")
                        .content("{\"topic\":\"ASISTENCIA\",\"action\":\"CREADA\",\"data\":{\"id\":42}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Evento reactivo emitido a 1 suscriptores."));

        hub.sendHeartbeat();
        final String events = stream.getResponse().getContentAsString();
        assertTrue(events.contains("event:CONNECTED"));
        assertTrue(events.contains("cliente-1"));
        assertTrue(events.contains("event:DATA_CHANGE"));
        assertTrue(events.contains("ASISTENCIA"));
        assertTrue(events.contains("CREADA"));
        assertTrue(events.contains("event:PING"));
    }

    @Test
    void statusAndDefaultPublicationWorkWithoutSubscribers() throws Exception {
        final RealtimeEventHub hub = new RealtimeEventHub();
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RealtimeEventsController(hub)).build();

        mvc.perform(get("/api/v1/realtime/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.activeSubscribers").value(0))
                .andExpect(jsonPath("$.datos.status").value("ONLINE"));

        mvc.perform(post("/api/v1/realtime/emit").contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Evento reactivo emitido a 0 suscriptores."));
        hub.sendHeartbeat();
        assertEquals(0, hub.getActiveSubscribersCount());
    }
}
