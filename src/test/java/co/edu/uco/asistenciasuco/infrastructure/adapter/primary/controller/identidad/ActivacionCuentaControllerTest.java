package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.identidad.activacioncuenta.primaryports.ReenviarActivacionCuentaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.jackson.JacksonInputConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** IDENT-002: POST /api/v1/identidad/cuentas/activacion/reenvios con el correo en el cuerpo. */
class ActivacionCuentaControllerTest {

    private static final String RUTA = "/api/v1/identidad/cuentas/activacion/reenvios";
    private static final UUID ACTOR = UUID.randomUUID();
    private static final String CORREO = "jorge.henao@uco.edu.co";

    private final ReenviarActivacionCuentaInputPort port = mock(ReenviarActivacionCuentaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ActivacionCuentaController(port, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
            .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper()))
            .build();

    private static JsonMapper mapper() {
        final JsonMapper.Builder builder = JsonMapper.builder();
        try (var context = new AnnotationConfigApplicationContext(JacksonInputConfig.class)) {
            context.getBean(JsonMapperBuilderCustomizer.class).customize(builder);
        }
        return builder.build();
    }

    @Test
    void reenvio_traslada_al_actor_autenticado_y_devuelve_el_mensaje() throws Exception {
        when(port.reenviar(ACTOR, CORREO)).thenReturn("Se envio de nuevo el enlace de activacion.");

        mvc.perform(post(RUTA).contentType("application/json").content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.mensaje").value("Se envio de nuevo el enlace de activacion."));
        verify(port).reenviar(ACTOR, CORREO);
    }

    @Test
    void correo_ausente_o_invalido_se_rechaza_antes_de_llamar_al_puerto() throws Exception {
        mvc.perform(post(RUTA).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post(RUTA).contentType("application/json").content("{\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }

    @Test
    void cuenta_ya_activa_responde_409() throws Exception {
        when(port.reenviar(ACTOR, CORREO)).thenThrow(new ConflictException("ACT_002", "La cuenta ya esta activa."));

        mvc.perform(post(RUTA).contentType("application/json").content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void los_colaboradores_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ActivacionCuentaController(null, identity));
        assertThrows(NullPointerException.class, () -> new ActivacionCuentaController(port, null));
    }
}
