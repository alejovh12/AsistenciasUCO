package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.ConsultarFichaDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.GrupoHistorialDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ResumenCargaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.ConsultarResumenDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenTipoVinculacionDTO;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU094/HU095: GET /api/v1/coordinador/docentes/{idDocente}/ficha (FC-01..FC-05) y /resumen (RC-01..RC-02). */
class CoordinadorDocentesReporteControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final String RUTA = "/api/v1/coordinador/docentes/" + DOCENTE + "/ficha";

    private final ConsultarFichaDocenteInputPort ficha = mock(ConsultarFichaDocenteInputPort.class);
    private final ConsultarResumenDocentesInputPort resumen = mock(ConsultarResumenDocentesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CoordinadorDocentesReporteController(ficha, resumen, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static FichaDocenteDTO respuesta() {
        final DocenteProgramaDTO docente = new DocenteProgramaDTO(UUID.randomUUID(), DOCENTE, UUID.randomUUID(), "CC", 1234, "Ana", null,
                "Perez", null, "Ana Perez", "ana@uco.edu.co", UUID.randomUUID(), "CAT", "Catedra", Instant.parse("2026-01-10T00:00:00Z"), 3, true);
        final GrupoHistorialDocenteDTO grupo = new GrupoHistorialDocenteDTO(UUID.randomUUID(), 7, "Grupo 7", UUID.randomUUID(), "Algebra",
                PERIODO, 20261, "2026-1", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 10), "VIGENTE", 30, 25, 0, 2, 1);
        return new FichaDocenteDTO(docente, new ResumenCargaDocenteDTO(4, 3, 1, 25), List.of(grupo), 4);
    }

    @Test
    void FC01_get_usa_el_actor_autenticado_valores_por_defecto_y_serializa_la_ficha() throws Exception {
        when(ficha.execute(any())).thenReturn(respuesta());

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.docente.idDocente").value(DOCENTE.toString()))
                .andExpect(jsonPath("$.datos.docente.nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.datos.resumen.totalGrupos").value(4))
                .andExpect(jsonPath("$.datos.resumen.estudiantesActivosVigentes").value(25))
                .andExpect(jsonPath("$.datos.historial[0].estadoPeriodo").value("VIGENTE"))
                .andExpect(jsonPath("$.datos.historial[0].nombreAsignatura").value("Algebra"))
                .andExpect(jsonPath("$.datos.totalHistorial").value(4));

        final ArgumentCaptor<ConsultarFichaDocenteDTO> dto = ArgumentCaptor.forClass(ConsultarFichaDocenteDTO.class);
        verify(ficha).execute(dto.capture());
        assertEquals(ACTOR, dto.getValue().actorUsuarioId());
        assertEquals(DOCENTE, dto.getValue().idDocente());
        assertNull(dto.getValue().idPeriodoAcademico());
        assertEquals(1, dto.getValue().pagina());
        assertEquals(10, dto.getValue().tamano());
    }

    @Test
    void FC02_propaga_periodo_y_paginacion() throws Exception {
        when(ficha.execute(any())).thenReturn(respuesta());

        mvc.perform(get(RUTA).param("idPeriodoAcademico", PERIODO.toString()).param("pagina", "3").param("tamano", "20"))
                .andExpect(status().isOk());

        final ArgumentCaptor<ConsultarFichaDocenteDTO> dto = ArgumentCaptor.forClass(ConsultarFichaDocenteDTO.class);
        verify(ficha).execute(dto.capture());
        assertEquals(PERIODO, dto.getValue().idPeriodoAcademico());
        assertEquals(3, dto.getValue().pagina());
        assertEquals(20, dto.getValue().tamano());
    }

    @Test
    void FC03_paginacion_fuera_de_rango_responde_400_sin_llamar_al_caso_de_uso() throws Exception {
        mvc.perform(get(RUTA).param("tamano", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(RUTA).param("tamano", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(RUTA).param("pagina", "0")).andExpect(status().isBadRequest());
        verifyNoInteractions(ficha);
    }

    @Test
    void FC04_docente_fuera_del_programa_responde_404() throws Exception {
        when(ficha.execute(any())).thenThrow(new ResourceNotFoundException("El docente no existe o no esta vinculado a su programa."));

        mvc.perform(get(RUTA)).andExpect(status().isNotFound());
    }

    @Test
    void FC05_constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new CoordinadorDocentesReporteController(null, resumen, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorDocentesReporteController(ficha, null, identity));
        assertThrows(NullPointerException.class, () -> new CoordinadorDocentesReporteController(ficha, resumen, null));
    }

    @Test
    void RC01_resumen_usa_el_actor_autenticado_y_serializa_totales_y_tipos() throws Exception {
        when(resumen.execute(ACTOR)).thenReturn(new ResumenDocentesProgramaDTO(5, 3, 2,
                List.of(new ResumenTipoVinculacionDTO(UUID.randomUUID(), "CAT", "Catedra", 2, 1, 3))));

        mvc.perform(get("/api/v1/coordinador/docentes/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.totalDocentes").value(5))
                .andExpect(jsonPath("$.datos.totalActivos").value(3))
                .andExpect(jsonPath("$.datos.totalInactivos").value(2))
                .andExpect(jsonPath("$.datos.porTipoVinculacion[0].codigoTipoVinculacion").value("CAT"))
                .andExpect(jsonPath("$.datos.porTipoVinculacion[0].total").value(3));
        verifyNoInteractions(ficha);
    }

    @Test
    void RC02_resumen_sin_programa_responde_403() throws Exception {
        when(resumen.execute(ACTOR)).thenThrow(new ForbiddenException("Sin programa."));

        mvc.perform(get("/api/v1/coordinador/docentes/resumen")).andExpect(status().isForbidden());
    }
}
