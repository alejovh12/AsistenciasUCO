package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoprograma;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TipoProgramaControllerTest {

    @Test
    void consulta_transversal_devuelve_solo_los_activos() throws Exception {
        final ConsultarTiposProgramaInputPort inputPort = mock(ConsultarTiposProgramaInputPort.class);
        when(inputPort.consultarActivos()).thenReturn(List.of(
                new TipoProgramaDTO(UUID.randomUUID(), "PRE", "pregrado", true),
                new TipoProgramaDTO(UUID.randomUUID(), "POS", "postgrado", true)));
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TipoProgramaController(inputPort)).build();

        mvc.perform(get("/api/v1/tipos-programa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[0].codigo").value("PRE"));

        verify(inputPort).consultarActivos();
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new TipoProgramaController(null));
    }
}
