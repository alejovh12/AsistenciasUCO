package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.verificacioncorreo;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.ConsultarConfiguracionRegistroEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.ReenviarVerificacionCorreoInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.VerificarCorreoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IDENT-001 BE-C01: los tres endpoints nuevos (verificacion publica, reenvio y configuracion del registro). La
 * autorizacion por rol se prueba en {@code AccessCatalogConsistencyTest}; aqui el contrato HTTP de cada controlador.
 */
class VerificacionCorreoControllersTest {

    private static final String TOKEN = "A".repeat(43);
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final VerificarCorreoInputPort verificar = mock(VerificarCorreoInputPort.class);
    private final ReenviarVerificacionCorreoInputPort reenviar = mock(ReenviarVerificacionCorreoInputPort.class);
    private final ConsultarConfiguracionRegistroEstudianteInputPort configuracion =
            mock(ConsultarConfiguracionRegistroEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new VerificacionCorreoController(verificar),
                    new GrupoVerificacionCorreoController(reenviar, identity),
                    new ConfiguracionRegistroEstudianteController(configuracion))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    // ---- POST /api/v1/public/verificar-correo -----------------------------------------------------------------

    @Test
    void verificar_con_token_valido_responde_200_con_el_mensaje_del_catalogo() throws Exception {
        when(verificar.execute(TOKEN)).thenReturn("Su correo fue verificado. Ya puede iniciar sesión.");

        mvc.perform(post("/api/v1/public/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.mensaje").value("Su correo fue verificado. Ya puede iniciar sesión."));
    }

    @Test
    void verificar_con_token_invalido_responde_400_con_el_mensaje_uniforme() throws Exception {
        when(verificar.execute(TOKEN)).thenThrow(new ValidationException("VCE_002", "El enlace de verificación no es válido o ya venció."));

        mvc.perform(post("/api/v1/public/verificar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace de verificación no es válido o ya venció."));
    }

    @Test
    void verificar_sin_cuerpo_o_sin_token_delega_nulo_para_que_Application_responda_igual() throws Exception {
        when(verificar.execute(null)).thenThrow(new ValidationException("VCE_002", "El enlace de verificación no es válido o ya venció."));

        mvc.perform(post("/api/v1/public/verificar-correo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace de verificación no es válido o ya venció."));
    }

    // ---- POST /api/v1/grupos/{grupoId}/estudiantes/{estudianteId}/reenviar-verificacion -------------------------

    @Test
    void reenviar_usa_el_actor_autenticado_y_responde_200() throws Exception {
        mvc.perform(post("/api/v1/grupos/{g}/estudiantes/{e}/reenviar-verificacion", GRUPO, ESTUDIANTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(reenviar).execute(ACTOR, GRUPO, ESTUDIANTE);
    }

    @Test
    void reenviar_traduce_los_rechazos_de_la_db() throws Exception {
        doThrow(new ResourceNotFoundException("VCE_004", "El estudiante no pertenece a un grupo a su cargo.")).when(reenviar)
                .execute(ACTOR, GRUPO, ESTUDIANTE);
        mvc.perform(post("/api/v1/grupos/{g}/estudiantes/{e}/reenviar-verificacion", GRUPO, ESTUDIANTE))
                .andExpect(status().isNotFound());

        doThrow(new ConflictException("VCE_005", "El correo de este estudiante ya fue verificado.")).when(reenviar)
                .execute(ACTOR, GRUPO, ESTUDIANTE);
        mvc.perform(post("/api/v1/grupos/{g}/estudiantes/{e}/reenviar-verificacion", GRUPO, ESTUDIANTE))
                .andExpect(status().isConflict());
    }

    @Test
    void reenviar_con_id_invalido_es_400_sin_invocar_el_caso_de_uso() throws Exception {
        mvc.perform(post("/api/v1/grupos/{g}/estudiantes/{e}/reenviar-verificacion", "no-es-uuid", ESTUDIANTE))
                .andExpect(status().isBadRequest());

        verify(reenviar, never()).execute(ACTOR, GRUPO, ESTUDIANTE);
    }

    // ---- GET /api/v1/configuracion/registro-estudiante -------------------------------------------------------

    @Test
    void configuracion_devuelve_el_dominio_vigente() throws Exception {
        when(configuracion.execute()).thenReturn("@uco.edu.co");

        mvc.perform(get("/api/v1/configuracion/registro-estudiante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.dominioCorreo").value("@uco.edu.co"));
    }
}
