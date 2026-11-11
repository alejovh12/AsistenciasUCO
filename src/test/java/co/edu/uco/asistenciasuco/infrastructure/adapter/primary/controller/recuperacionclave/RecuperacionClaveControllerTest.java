package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.RestablecerClaveInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.recuperarclave.primaryports.SolicitarRecuperacionClaveInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request.RestablecerClaveRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.recuperacionclave.request.SolicitarRecuperacionClaveRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU177: contrato HTTP de las dos rutas publicas de recuperacion de contrasena. */
class RecuperacionClaveControllerTest {

    private static final String SOLICITUD = "/api/v1/public/recuperacion-clave/solicitud";
    private static final String CONFIRMACION = "/api/v1/public/recuperacion-clave/confirmacion";
    private static final String TOKEN = "A".repeat(43);
    private static final String MENSAJE_SOLICITUD = "Si el correo está registrado, recibirá un enlace para definir una contraseña nueva.";
    private static final String MENSAJE_INVALIDO = "El enlace para definir la contraseña no es válido o ya venció. Solicite uno nuevo.";

    private final SolicitarRecuperacionClaveInputPort solicitar = mock(SolicitarRecuperacionClaveInputPort.class);
    private final RestablecerClaveInputPort restablecer = mock(RestablecerClaveInputPort.class);

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RecuperacionClaveController(solicitar, restablecer))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void solicitar_responde_200_con_el_mensaje_uniforme() throws Exception {
        when(solicitar.execute("ana.perez@uco.edu.co")).thenReturn(MENSAJE_SOLICITUD);

        mvc.perform(post(SOLICITUD).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"ana.perez@uco.edu.co\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.mensaje").value(MENSAJE_SOLICITUD));
    }

    @Test
    void solicitar_sin_cuerpo_delega_nulo_y_responde_igual() throws Exception {
        when(solicitar.execute(null)).thenReturn(MENSAJE_SOLICITUD);

        mvc.perform(post(SOLICITUD).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensaje").value(MENSAJE_SOLICITUD));
    }

    @Test
    void confirmar_con_enlace_valido_responde_200_con_el_mensaje_del_catalogo() throws Exception {
        when(restablecer.execute(TOKEN, "ClaveNueva-2026")).thenReturn("Su contraseña fue actualizada. Ya puede iniciar sesión.");

        mvc.perform(post(CONFIRMACION).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"clave\":\"ClaveNueva-2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.mensaje").value("Su contraseña fue actualizada. Ya puede iniciar sesión."));
    }

    @Test
    void confirmar_con_enlace_que_no_sirve_responde_400_con_el_mensaje_uniforme() throws Exception {
        when(restablecer.execute(TOKEN, "ClaveNueva-2026")).thenThrow(new ValidationException("RCL_002", MENSAJE_INVALIDO));

        mvc.perform(post(CONFIRMACION).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\",\"clave\":\"ClaveNueva-2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MENSAJE_INVALIDO));
    }

    @Test
    void confirmar_sin_cuerpo_delega_nulos_para_que_Application_responda_igual() throws Exception {
        when(restablecer.execute(null, null)).thenThrow(new ValidationException("RCL_002", MENSAJE_INVALIDO));

        mvc.perform(post(CONFIRMACION).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(MENSAJE_INVALIDO));
    }

    @Test
    void los_cuerpos_nunca_imprimen_el_correo_el_token_ni_la_clave() {
        final SolicitarRecuperacionClaveRequest solicitud = new SolicitarRecuperacionClaveRequest();
        solicitud.setCorreo("ana.perez@uco.edu.co");
        final RestablecerClaveRequest confirmacion = new RestablecerClaveRequest();
        confirmacion.setToken(TOKEN);
        confirmacion.setClave("ClaveNueva-2026");

        assertFalse(solicitud.toString().contains("ana.perez"));
        assertFalse(confirmacion.toString().contains(TOKEN));
        assertFalse(confirmacion.toString().contains("ClaveNueva"));
    }
}
