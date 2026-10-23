package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoprograma;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.ConsultarCatalogosProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** T-023-01: catalogos de programa expuestos como listas simples (mismo patron que tipos-identificacion). */
class CatalogoProgramaControllerTest {

    private final ConsultarCatalogosProgramaInputPort inputPort = mock(ConsultarCatalogosProgramaInputPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CatalogoProgramaController(inputPort)).build();

    @Test
    void tipos_programa_responde_200_con_id_codigo_y_nombre() throws Exception {
        when(inputPort.tiposPrograma()).thenReturn(List.of(
                new CatalogoProgramaDTO(UUID.randomUUID(), "PRE", "pregrado"),
                new CatalogoProgramaDTO(UUID.randomUUID(), "POS", "postgrado")));

        mvc.perform(get("/api/v1/tipos-programa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("PRE"))
                .andExpect(jsonPath("$[1].nombre").value("postgrado"));
    }

    @Test
    void modalidades_responde_200() throws Exception {
        when(inputPort.modalidades()).thenReturn(List.of(new CatalogoProgramaDTO(UUID.randomUUID(), "VIR", "Virtual")));

        mvc.perform(get("/api/v1/modalidades-programa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("VIR"));
    }

    @Test
    void jornadas_responde_200() throws Exception {
        when(inputPort.jornadas()).thenReturn(List.of(new CatalogoProgramaDTO(UUID.randomUUID(), "NOC", "Nocturna")));

        mvc.perform(get("/api/v1/jornadas-programa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Nocturna"));
    }
}
