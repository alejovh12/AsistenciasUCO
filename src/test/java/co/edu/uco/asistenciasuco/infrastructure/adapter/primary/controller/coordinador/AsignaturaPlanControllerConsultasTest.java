package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.AsignaturaPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.*;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CULM-001 B2NM: endpoints de consulta paginados por SP (malla, asignaturas, planes del coordinador y grupos del decano). */
class AsignaturaPlanControllerConsultasTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final AsignaturaPlanInputPort inputPort = mock(AsignaturaPlanInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsignaturaPlanController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static AsignaturaPlanDTO fila() {
        return new AsignaturaPlanDTO(ID, "MAT1", "Calculo", 3, ID, "Ciencias", ID, "Basico", PLAN, 2024, PROGRAMA, "Sistemas", ID, 2, "S2",
                "OPT", "Optativa", true, true, true);
    }

    @Test
    void malla_del_coordinador_y_del_decano_comparten_contrato_paginado() throws Exception {
        when(inputPort.consultarMalla(ACTOR, PLAN, 2, "OPT", "cal", 3, 10)).thenReturn(new PaginaAsignaturasPlanDTO(List.of(fila()), 31));

        for (final String rol : List.of("coordinador", "decano")) {
            mvc.perform(get("/api/v1/" + rol + "/planes-estudio/{planId}/asignaturas", PLAN).param("semestre", "2").param("caracter", "OPT")
                            .param("busqueda", "cal").param("pagina", "3").param("tamano", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitoso").value(true))
                    .andExpect(jsonPath("$.numeroPagina").value(3))
                    .andExpect(jsonPath("$.tamanoPagina").value(10))
                    .andExpect(jsonPath("$.totalElementos").value(31))
                    .andExpect(jsonPath("$.totalPaginas").value(4))
                    .andExpect(jsonPath("$.elementos[0].idAsignatura").value(ID.toString()))
                    .andExpect(jsonPath("$.elementos[0].codigoCaracter").value("OPT"))
                    .andExpect(jsonPath("$.elementos[0].nombreCaracter").value("Optativa"))
                    .andExpect(jsonPath("$.elementos[0].numeroSemestre").value(2))
                    .andExpect(jsonPath("$.elementos[0].nombrePrograma").value("Sistemas"))
                    .andExpect(jsonPath("$.elementos[0].compartida").value(true))
                    .andExpect(jsonPath("$.elementos[0].estaActivoVinculo").value(true));
        }
    }

    @Test
    void malla_de_plan_ajeno_o_inexistente_es_404() throws Exception {
        when(inputPort.consultarMalla(ACTOR, PLAN, null, null, null, 1, 20)).thenThrow(new ResourceNotFoundException("PLA_002"));

        mvc.perform(get("/api/v1/coordinador/planes-estudio/{planId}/asignaturas", PLAN)).andExpect(status().isNotFound());
    }

    @Test
    void asignaturas_del_coordinador_filtra_por_programa_y_estado() throws Exception {
        when(inputPort.consultarAsignaturasCoordinador(ACTOR, PROGRAMA, "x", false, 1, 20)).thenReturn(new PaginaAsignaturasPlanDTO(List.of(fila()), 1));

        mvc.perform(get("/api/v1/coordinador/asignaturas").param("programaId", PROGRAMA.toString()).param("busqueda", "x").param("estaActiva", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.elementos[0].codigo").value("MAT1"));
        verify(inputPort).consultarAsignaturasCoordinador(ACTOR, PROGRAMA, "x", false, 1, 20);
    }

    @Test
    void asignaturas_de_programa_ajeno_es_403() throws Exception {
        when(inputPort.consultarAsignaturasCoordinador(ACTOR, PROGRAMA, null, null, 1, 20)).thenThrow(new ForbiddenException("COO_003"));

        mvc.perform(get("/api/v1/coordinador/asignaturas").param("programaId", PROGRAMA.toString())).andExpect(status().isForbidden());
    }

    @Test
    void planes_del_coordinador_expone_totales_y_estado() throws Exception {
        when(inputPort.consultarPlanes(ACTOR, PROGRAMA, true, 1, 20)).thenReturn(new PaginaPlanesEstudioDTO(
                List.of(new PlanEstudioDTO(PLAN, PROGRAMA, "Sistemas", "2024", true, "Activo", null, 10, 40, 120)), 1));

        mvc.perform(get("/api/v1/coordinador/planes-estudio").param("programaId", PROGRAMA.toString()).param("estaActivo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].id").value(PLAN.toString()))
                .andExpect(jsonPath("$.elementos[0].nombrePrograma").value("Sistemas"))
                .andExpect(jsonPath("$.elementos[0].estaActivoPlanEstudio").value(true))
                .andExpect(jsonPath("$.elementos[0].estaActivoTextoPlanEstudio").value("Activo"))
                .andExpect(jsonPath("$.elementos[0].totalSemestres").value(10))
                .andExpect(jsonPath("$.elementos[0].totalAsignaturas").value(40))
                .andExpect(jsonPath("$.elementos[0].totalCreditos").value(120));
    }

    @Test
    void grupos_del_decano_exponen_programas_como_lista_y_no_dentro_de_otro_campo() throws Exception {
        when(inputPort.consultarGruposFacultad(ACTOR, PERIODO, PROGRAMA, "calc", 2, 5)).thenReturn(new PaginaGruposFacultadDTO(List.of(
                new GrupoFacultadDTO(ID, 931001, "G1", PERIODO, ID, "Calculo", ID, 30, 12, 18, true, List.of("Sistemas", "Industrial"))), 6));

        mvc.perform(get("/api/v1/decano/grupos").param("idPeriodoAcademico", PERIODO.toString()).param("programaId", PROGRAMA.toString())
                        .param("busqueda", "calc").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.elementos[0].idGrupo").value(ID.toString()))
                .andExpect(jsonPath("$.elementos[0].codigo").value(931001))
                .andExpect(jsonPath("$.elementos[0].estudiantesActivos").value(12))
                .andExpect(jsonPath("$.elementos[0].cuposDisponibles").value(18))
                .andExpect(jsonPath("$.elementos[0].programas[0]").value("Sistemas"))
                .andExpect(jsonPath("$.elementos[0].programas[1]").value("Industrial"))
                .andExpect(jsonPath("$.elementos[0].nombrePrograma").doesNotExist());
    }
}
