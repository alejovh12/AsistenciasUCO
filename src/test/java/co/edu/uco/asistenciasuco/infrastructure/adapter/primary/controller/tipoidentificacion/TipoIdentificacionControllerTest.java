package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tipoidentificacion;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TipoIdentificacionControllerTest {

    @Test
    void consultar_tipos_identificacion_responde_200_con_los_codigos_esperados() throws Exception {
        final ConsultarTiposIdentificacionInputPort inputPort = () -> List.of(
                new TipoIdentificacionDTO(
                        UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                        "CC",
                        "Cédula de ciudadanía"
                ),
                new TipoIdentificacionDTO(
                        UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d"),
                        "PA",
                        "Pasaporte"
                ),
                new TipoIdentificacionDTO(
                        UUID.fromString("67a3d514-ab1b-4ce7-b661-1a99ea18fd9d"),
                        "TI",
                        "Tarjeta de identidad"
                )
        );

        final MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TipoIdentificacionController(inputPort))
                .build();

        mockMvc.perform(get("/api/v1/tipos-identificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tipoIdentificacion").value("CC"))
                .andExpect(jsonPath("$[1].tipoIdentificacion").value("PA"))
                .andExpect(jsonPath("$[2].tipoIdentificacion").value("TI"));
    }
}
