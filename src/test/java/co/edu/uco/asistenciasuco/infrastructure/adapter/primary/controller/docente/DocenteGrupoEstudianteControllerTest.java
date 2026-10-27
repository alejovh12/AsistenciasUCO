package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.RetirarEstudianteGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto.RetirarEstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU051: {@code POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro} (CT-01..CT-04). */
class DocenteGrupoEstudianteControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final String RUTA = "/api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro";

    private final RetirarEstudianteGrupoInputPort retirar = mock(RetirarEstudianteGrupoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocenteGrupoEstudianteController(retirar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void CT01_retira_con_el_actor_autenticado_y_el_motivo() throws Exception {
        mvc.perform(post(RUTA, GRUPO, ESTUDIANTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Inasistencia reiterada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(retirar).execute(new RetirarEstudianteGrupoDTO(ACTOR, GRUPO, ESTUDIANTE, "Inasistencia reiterada"));
    }

    @Test
    void CT02_sin_cuerpo_delega_motivo_nulo_para_que_Application_lo_rechace() throws Exception {
        mvc.perform(post(RUTA, GRUPO, ESTUDIANTE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        verify(retirar).execute(new RetirarEstudianteGrupoDTO(ACTOR, GRUPO, ESTUDIANTE, null));
    }

    @Test
    void CT03_usuario_no_docente_es_403() throws Exception {
        doThrow(new ForbiddenException("No fue posible resolver el docente autenticado.")).when(retirar)
                .execute(new RetirarEstudianteGrupoDTO(ACTOR, GRUPO, ESTUDIANTE, "Motivo"));

        mvc.perform(post(RUTA, GRUPO, ESTUDIANTE).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void CT04_grupo_ajeno_o_sin_matricula_es_404_e_id_invalido_es_400() throws Exception {
        doThrow(new ResourceNotFoundException("No existe una matricula activa del estudiante en un grupo suyo.")).when(retirar)
                .execute(new RetirarEstudianteGrupoDTO(ACTOR, GRUPO, ESTUDIANTE, "Motivo"));

        mvc.perform(post(RUTA, GRUPO, ESTUDIANTE).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(RUTA, "no-es-uuid", ESTUDIANTE).contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isBadRequest());
    }
}
