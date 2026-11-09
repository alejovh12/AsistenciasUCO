package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.AsignaturaAsociableDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.AsociarAsignaturaPlanDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.CaracterAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.PaginaAsignaturasAsociablesDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.ProgramaResumenDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CULM-001 B2NM (DEC-019): comandos y consultas simples de asignaturas por plan (asociar, desvincular, asociables, caracteres, programas de un grupo). */
class AsignaturaPlanControllerComandosTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final AsignaturaPlanInputPort inputPort = mock(AsignaturaPlanInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsignaturaPlanController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void asociar_responde_201_y_delega_el_cuerpo_con_el_usuario_autenticado() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas", PLAN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsignatura\":\"" + ASIGNATURA + "\",\"semestreNumero\":3,\"codigoCaracter\":\"OBL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(inputPort).asociar(ACTOR, PLAN, new AsociarAsignaturaPlanDTO(ASIGNATURA, 3, "OBL"));
    }

    @Test
    void asociar_con_un_plan_ajeno_responde_403() throws Exception {
        doThrow(new ForbiddenException("SEC_002", "No tiene acceso a este plan.")).when(inputPort).asociar(ACTOR, PLAN, new AsociarAsignaturaPlanDTO(ASIGNATURA, 1, "OBL"));

        mvc.perform(post("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociadas", PLAN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idAsignatura\":\"" + ASIGNATURA + "\",\"semestreNumero\":1,\"codigoCaracter\":\"OBL\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SEC_002"));
    }

    @Test
    void desvincular_responde_200_y_delega() throws Exception {
        mvc.perform(delete("/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}", PLAN, ASIGNATURA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(inputPort).desvincular(ACTOR, PLAN, ASIGNATURA);
    }

    @Test
    void asociables_responde_la_pagina_con_los_filtros_enviados() throws Exception {
        when(inputPort.consultarAsociables(ACTOR, PLAN, "cal", 2, 5)).thenReturn(new PaginaAsignaturasAsociablesDTO(
                List.of(new AsignaturaAsociableDTO(ASIGNATURA, "MAT1", "Calculo", 3, "Ciencias", "Basico", "Plan 2024")), 11));

        mvc.perform(get("/api/v1/coordinador/planes-estudio/{planId}/asignaturas-asociables", PLAN)
                        .param("busqueda", "cal").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(11))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.elementos[0].codigo").value("MAT1"));
    }

    @Test
    void caracteres_y_programas_del_grupo_responden_listas_con_su_total() throws Exception {
        when(inputPort.consultarCaracteres()).thenReturn(List.of(new CaracterAsignaturaDTO("OBL", "Obligatoria", 1), new CaracterAsignaturaDTO("ELE", "Electiva", 2)));
        when(inputPort.consultarProgramasGrupo(ACTOR, GRUPO)).thenReturn(List.of(new ProgramaResumenDTO(UUID.randomUUID(), "Sistemas", PLAN, 2024)));

        mvc.perform(get("/api/v1/coordinador/caracteres-asignatura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[1].codigo").value("ELE"));
        mvc.perform(get("/api/v1/grupos/{grupoId}/programas", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].nombrePrograma").value("Sistemas"));
    }
}
