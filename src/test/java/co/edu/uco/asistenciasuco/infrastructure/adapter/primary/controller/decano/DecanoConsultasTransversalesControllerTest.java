package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.MatriculaHistorialDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ProgramaCreditosDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ResumenAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU112/HU128/HU143/HU157: endpoints GET de solo lectura del decano. */
class DecanoConsultasTransversalesControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DecanoConsultasTransversalesController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_112_01_get_programas_creditos() throws Exception {
        when(inputPort.consultarProgramasCreditos(ACTOR, "sis", true, 1, 20)).thenReturn(new PaginaDTO<>(List.of(
                new ProgramaCreditosDTO(ID, "Sistemas", "Pregrado", "Presencial", "Diurna", true, ID, 2024, true, 10,
                        52, 160)), 3));

        mvc.perform(get("/api/v1/decano/programas-creditos").param("busqueda", "sis").param("estaActivo", "true")
                        .param("pagina", "1").param("tamano", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.elementos[0].totalCreditos").value(160))
                .andExpect(jsonPath("$.elementos[0].inpPlanEstudio").value(2024));
    }

    @Test
    void T_112_02_sin_perfil_decano_es_403() throws Exception {
        when(inputPort.consultarProgramasCreditos(eq(ACTOR), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new ForbiddenException("SEC_001", "No tiene el perfil requerido para esta consulta."));

        mvc.perform(get("/api/v1/decano/programas-creditos")).andExpect(status().isForbidden());
    }

    @Test
    void T_128_01_get_docentes_filtrados_por_programa() throws Exception {
        when(inputPort.consultarDocentes(ACTOR, null, ID, null, null, null)).thenReturn(new PaginaDTO<>(List.of(
                new DocenteFacultadDTO(ID, "101", "Ana", "ana@uco.edu.co", true, "Sistemas", 1, 4)), 1));

        mvc.perform(get("/api/v1/decano/docentes").param("idPrograma", ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].programas").value("Sistemas"))
                .andExpect(jsonPath("$.elementos[0].totalGrupos").value(4));
    }

    @Test
    void T_128_02_paginacion_invalida_es_400() throws Exception {
        when(inputPort.consultarDocentes(ACTOR, null, null, null, 0, null))
                .thenThrow(new ValidationException("La pagina debe ser mayor o igual a 1."));

        mvc.perform(get("/api/v1/decano/docentes").param("pagina", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void T_143_01_get_asignaturas_de_la_facultad_sin_filtro_de_facultad() throws Exception {
        when(inputPort.buscarAsignaturas(ACTOR, "CAL", null, true, null, null)).thenReturn(new PaginaDTO<>(List.of(
                new AsignaturaInstitucionDTO(ID, "CAL1", "Calculo", 3, "Ciencias", "Basico",
                        "Sistemas · Plan 2024 · S1", 1, true)), 1));

        mvc.perform(get("/api/v1/decano/asignaturas").param("busqueda", "CAL").param("estaActiva", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos[0].codigo").value("CAL1"))
                .andExpect(jsonPath("$.elementos[0].creditos").value(3))
                .andExpect(jsonPath("$.elementos[0].planes").value("Sistemas · Plan 2024 · S1"))
                .andExpect(jsonPath("$.elementos[0].totalPlanes").value(1));
    }

    @Test
    void T_157_01_get_historial_del_estudiante() throws Exception {
        when(inputPort.consultarHistorial(ACTOR, ID)).thenReturn(new FichaUnificadaDTO(ID, "CC", "101", "Ana Ruiz",
                "ana@uco.edu.co", true, List.of(new MatriculaHistorialDTO(ID, "Ingenieria", ID, "Sistemas", 2024, ID,
                "2026-1", "Primer semestre", ID, 1, ID, "Calculo", "MAT", "Matriculado",
                new ResumenAsistenciaDTO(10, 8, 2, 0, new BigDecimal("20.00"), "RIESGO")))));

        mvc.perform(get("/api/v1/decano/estudiantes/{id}/historial", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombreCompleto").value("Ana Ruiz"))
                .andExpect(jsonPath("$.datos.matriculas[0].resumen.codigoPermanencia").value("RIESGO"));
    }

    @Test
    void T_157_02_historial_fuera_del_ambito_es_404() throws Exception {
        when(inputPort.consultarHistorial(ACTOR, ID))
                .thenThrow(new ResourceNotFoundException("El estudiante no existe en su ambito."));

        mvc.perform(get("/api/v1/decano/estudiantes/{id}/historial", ID)).andExpect(status().isNotFound());
    }

    /** CULM-001 D.9-a (F-ADM-023): sobre ApiPageResponse y 400 PAG_001 en los tres listados del decano. */
    @Test
    void culm001_d9_docentes_responde_api_page_response_con_metadatos() throws Exception {
        when(inputPort.consultarDocentes(ACTOR, null, null, null, 1, 4)).thenReturn(new PaginaDTO<>(List.of(), 9));

        mvc.perform(get("/api/v1/decano/docentes").param("pagina", "1").param("tamano", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos").isArray())
                .andExpect(jsonPath("$.numeroPagina").value(1))
                .andExpect(jsonPath("$.tamanoPagina").value(4))
                .andExpect(jsonPath("$.totalElementos").value(9))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.datos").doesNotExist());
    }

    @Test
    void culm001_d9_paginacion_invalida_es_400_pag_001_en_cada_listado() throws Exception {
        for (final String ruta : new String[]{"/api/v1/decano/programas-creditos", "/api/v1/decano/docentes", "/api/v1/decano/asignaturas"}) {
            mvc.perform(get(ruta).param("pagina", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PAG_001"))
                    .andExpect(jsonPath("$.details[0].field").value("pagina"));
            mvc.perform(get(ruta).param("tamano", "100000"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PAG_001"))
                    .andExpect(jsonPath("$.details[0].field").value("tamano"));
        }
        org.mockito.Mockito.verifyNoInteractions(inputPort);
    }
}
