package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.ConsultarPlanEstudioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.AsignaturaPlanEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.SemestrePlanEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU018: GET /api/v1/estudiante/plan-estudio (PE-C-01..04). */
class EstudiantePlanEstudioControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/estudiante/plan-estudio";

    private final ConsultarPlanEstudioEstudianteInputPort consultar = mock(ConsultarPlanEstudioEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new EstudiantePlanEstudioController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static PlanEstudioEstudianteDTO plan() {
        final UUID grupo = UUID.randomUUID();
        return new PlanEstudioEstudianteDTO(UUID.randomUUID(), 2024, UUID.randomUUID(), "Ingenieria de Sistemas", 8, 1, 3, 1, 3,
                List.of(new SemestrePlanEstudianteDTO(UUID.randomUUID(), 8, 1, 3, List.of(
                                new AsignaturaPlanEstudianteDTO(UUID.randomUUID(), "ARQ-402", "Arquitectura de Software", 3, "ESPECIFICA",
                                        "OBL", "Obligatoria", true, grupo, "Grupo 001 - Arq Software"))),
                        new SemestrePlanEstudianteDTO(UUID.randomUUID(), 9, 0, 0, List.of())));
    }

    @Test
    void PE_C_01_get_usa_el_actor_autenticado_y_serializa_planes_semestres_y_asignaturas() throws Exception {
        when(consultar.execute(ACTOR)).thenReturn(List.of(plan()));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].nombrePrograma").value("Ingenieria de Sistemas"))
                .andExpect(jsonPath("$.datos[0].inp").value(2024))
                .andExpect(jsonPath("$.datos[0].totalSemestres").value(8))
                .andExpect(jsonPath("$.datos[0].totalAsignaturas").value(1))
                .andExpect(jsonPath("$.datos[0].totalCreditos").value(3))
                .andExpect(jsonPath("$.datos[0].asignaturasCursando").value(1))
                .andExpect(jsonPath("$.datos[0].creditosCursando").value(3))
                .andExpect(jsonPath("$.datos[0].semestres[0].numeroSemestre").value(8))
                .andExpect(jsonPath("$.datos[0].semestres[0].totalCreditos").value(3))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].codigo").value("ARQ-402"))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].nombre").value("Arquitectura de Software"))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].creditos").value(3))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].tipoAsignatura").value("ESPECIFICA"))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].nombreCaracter").value("Obligatoria"))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].cursando").value(true))
                .andExpect(jsonPath("$.datos[0].semestres[0].asignaturas[0].nombreGrupoCursando").value("Grupo 001 - Arq Software"))
                .andExpect(jsonPath("$.datos[0].semestres[1].numeroSemestre").value(9))
                .andExpect(jsonPath("$.datos[0].semestres[1].asignaturas").isEmpty());

        verify(consultar).execute(ACTOR);
    }

    @Test
    void PE_C_02_sin_planes_responde_200_con_la_lista_vacia() throws Exception {
        when(consultar.execute(ACTOR)).thenReturn(List.of());

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.datos").isEmpty());
    }

    @Test
    void PE_C_03_usuario_sin_estudiante_responde_403() throws Exception {
        when(consultar.execute(ACTOR)).thenThrow(new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        mvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    void PE_C_04_constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new EstudiantePlanEstudioController(null, identity));
        assertThrows(NullPointerException.class, () -> new EstudiantePlanEstudioController(consultar, null));
    }
}
