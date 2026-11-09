package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.configuracion;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.ConsultarConfiguracionReclamosInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
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

/** CULM-001 D.2 (CONTRACT D 3.3): GET /api/v1/configuracion/reclamos devuelve ApiDataResponse con la forma exacta del contrato. */
class ConfiguracionReclamosControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConsultarConfiguracionReclamosInputPort inputPort = mock(ConsultarConfiguracionReclamosInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConfiguracionReclamosController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void devuelve_la_configuracion_con_los_campos_del_contrato_y_usa_el_actor_autenticado() throws Exception {
        when(inputPort.execute(ACTOR)).thenReturn(
                new ConfiguracionReclamosDTO(3, 5_242_880L, List.of("application/pdf", "image/png", "image/jpeg"), 10, 5));

        mvc.perform(get("/api/v1/configuracion/reclamos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.maximoSoportes").value(3))
                .andExpect(jsonPath("$.datos.tamanioMaximoBytes").value(5_242_880))
                .andExpect(jsonPath("$.datos.tiposPermitidos[0]").value("application/pdf"))
                .andExpect(jsonPath("$.datos.tiposPermitidos[1]").value("image/png"))
                .andExpect(jsonPath("$.datos.tiposPermitidos[2]").value("image/jpeg"))
                .andExpect(jsonPath("$.datos.limiteSubidasPorMinuto").value(10))
                .andExpect(jsonPath("$.datos.diasLimiteJustificacion").value(5));
        verify(inputPort).execute(ACTOR);
    }

    @Test
    void un_rol_sin_acceso_se_traduce_a_403_desde_la_base_de_datos() throws Exception {
        when(inputPort.execute(ACTOR)).thenThrow(new ForbiddenException("sin acceso"));

        mvc.perform(get("/api/v1/configuracion/reclamos")).andExpect(status().isForbidden());
    }

    @Test
    void exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new ConfiguracionReclamosController(null, identity));
        assertThrows(NullPointerException.class, () -> new ConfiguracionReclamosController(inputPort, null));
    }
}
