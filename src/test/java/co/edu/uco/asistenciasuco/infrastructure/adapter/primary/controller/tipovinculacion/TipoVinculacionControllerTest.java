package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipovinculacion;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.ConsultarTiposVinculacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.dto.TipoVinculacionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU090: GET /api/v1/tipos-vinculacion (catalogo de DB, sin valores quemados en el front). */
class TipoVinculacionControllerTest {

    private final ConsultarTiposVinculacionInputPort inputPort = mock(ConsultarTiposVinculacionInputPort.class);

    @Test
    void constructor_rechaza_puerto_nulo() {
        assertThrows(NullPointerException.class, () -> new TipoVinculacionController(null));
    }

    @Test
    void T_090_01_lista_los_tipos_activos() throws Exception {
        final UUID id = UUID.randomUUID();
        when(inputPort.execute()).thenReturn(List.of(new TipoVinculacionDTO(id, "PLA", "Planta")));
        final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TipoVinculacionController(inputPort)).build();

        mvc.perform(get("/api/v1/tipos-vinculacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].codigo").value("PLA"))
                .andExpect(jsonPath("$[0].nombre").value("Planta"));
    }
}
