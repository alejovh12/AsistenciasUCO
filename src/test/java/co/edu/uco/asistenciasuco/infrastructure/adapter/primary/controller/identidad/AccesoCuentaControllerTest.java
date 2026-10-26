package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.AccesoCuentaInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoCuentaDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoRolDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.EstadoAccesoCuenta;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccesoCuentaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID DESTINO = UUID.randomUUID();
    private static final String CORREO = "persona@uco.edu.co";

    private final AccesoCuentaInputPort port = mock(AccesoCuentaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AccesoCuentaController(port, identity))
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

    private static AccesoCuentaDTO acceso(final EstadoAccesoCuenta estado) {
        return new AccesoCuentaDTO(DESTINO, estado, "detalle",
                List.of(new AccesoRolDTO("DOCENTE", true, true, "ASIGNADO")));
    }

    @Test
    void consulta_traslada_al_actor_autenticado_y_devuelve_estado_y_roles() throws Exception {
        when(port.consultar(ACTOR, CORREO)).thenReturn(acceso(EstadoAccesoCuenta.ACTIVA));

        mvc.perform(post("/api/v1/identidad/cuentas/acceso/consultas").contentType("application/json")
                        .content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.datos.usuario").value(DESTINO.toString()))
                .andExpect(jsonPath("$.datos.roles[0].rol").value("DOCENTE"))
                .andExpect(jsonPath("$.datos.roles[0].perfil").value("ASIGNADO"));
        verify(port).consultar(ACTOR, CORREO);
    }

    @Test
    void reconciliacion_traslada_al_actor_y_devuelve_el_estado_resultante() throws Exception {
        when(port.reconciliar(ACTOR, CORREO)).thenReturn(acceso(EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION));

        mvc.perform(post("/api/v1/identidad/cuentas/acceso/reconciliaciones").contentType("application/json")
                        .content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estado").value("PENDIENTE_SINCRONIZACION"));
        verify(port).reconciliar(ACTOR, CORREO);
    }

    @Test
    void correo_ausente_o_invalido_se_rechaza_antes_de_llamar_al_puerto() throws Exception {
        mvc.perform(post("/api/v1/identidad/cuentas/acceso/consultas").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/identidad/cuentas/acceso/reconciliaciones").contentType("application/json")
                        .content("{\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }

    @Test
    void campos_desconocidos_se_rechazan() throws Exception {
        mvc.perform(post("/api/v1/identidad/cuentas/acceso/consultas").contentType("application/json")
                        .content("{\"correo\":\"" + CORREO + "\",\"rol\":\"DECANO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(port);
    }

    @Test
    void cuenta_fuera_de_alcance_responde_403() throws Exception {
        when(port.consultar(ACTOR, CORREO)).thenThrow(new ForbiddenException("fuera de alcance"));

        mvc.perform(post("/api/v1/identidad/cuentas/acceso/consultas").contentType("application/json")
                        .content("{\"correo\":\"" + CORREO + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void el_correo_no_viaja_en_la_url_la_consulta_por_get_no_existe() throws Exception {
        mvc.perform(get("/api/v1/identidad/cuentas/acceso/consultas?correo=" + CORREO))
                .andExpect(status().isMethodNotAllowed());
    }
}
