package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.ConsultarComparativaProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ConsultarComparativaProgramasDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.ConsultarKpisDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.ConsultarKpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DecanoIndicadoresControllerTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final ConsultarKpisDecanoInputPort kpis = mock(ConsultarKpisDecanoInputPort.class);
    private final ConsultarComparativaProgramasInputPort comparativa = mock(ConsultarComparativaProgramasInputPort.class);
    private final ConsultarAforoGruposInputPort aforo = mock(ConsultarAforoGruposInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DecanoIndicadoresController(kpis, comparativa, aforo, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> "KPI_003".equals(codigo)
                    ? Optional.of("No tiene una facultad a su cargo para consultar estos indicadores.") : Optional.empty())).build();

    @Test
    void T_E2E_get_indicadores_serializa_cada_cifra_y_conserva_los_nulos() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisDecanoDTO(FACULTAD, "Ingenieria", PERIODO, "2026-2", 3, 6, 2, 1, 3, new BigDecimal("50.00"),
                35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 3, 55, 7, null, new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/decano/indicadores").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.totalProgramas").value(3))
                .andExpect(jsonPath("$.datos.totalEstudiantes").value(6))
                .andExpect(jsonPath("$.datos.estudiantesPerdida").value(3))
                .andExpect(jsonPath("$.datos.tasaRetencion").value(50.0))
                .andExpect(jsonPath("$.datos.porcentajeAusentismo").value(17.14))
                .andExpect(jsonPath("$.datos.porcentajeAsistencia").value(82.86))
                .andExpect(jsonPath("$.datos.gruposActivos").value(3))
                .andExpect(jsonPath("$.datos.cupoTotal").value(55))
                .andExpect(jsonPath("$.datos.tasaAforo").doesNotExist())
                .andExpect(jsonPath("$.datos.umbralPerdida").value(20.0));

        final ArgumentCaptor<ConsultarKpisDecanoDTO> captor = ArgumentCaptor.forClass(ConsultarKpisDecanoDTO.class);
        verify(kpis).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_indicadores_sin_periodo_delega_nulo() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisDecanoDTO(FACULTAD, "Ingenieria", null, null, 0, 0, 0, 0, 0, null, 0, 0, null, null, 0, 0, 0, null,
                new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/decano/indicadores")).andExpect(status().isOk());

        final ArgumentCaptor<ConsultarKpisDecanoDTO> captor = ArgumentCaptor.forClass(ConsultarKpisDecanoDTO.class);
        verify(kpis).execute(captor.capture());
        assertNull(captor.getValue().idPeriodoAcademico());
    }

    @Test
    void T_SEC_get_indicadores_sin_facultad_responde_403_con_el_mensaje_del_servidor() throws Exception {
        when(kpis.execute(any())).thenThrow(new ForbiddenException("KPI_003", "Ambito: el ejecutor no es decano."));

        mvc.perform(get("/api/v1/decano/indicadores"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene una facultad a su cargo para consultar estos indicadores."));
    }

    @Test
    void T_E2E_get_comparativa_devuelve_una_fila_por_programa_con_nulos() throws Exception {
        when(comparativa.execute(any())).thenReturn(List.of(
                new ComparativaProgramaDTO(PROGRAMA, "Programa 1", true, 5, 1, 2, new BigDecimal("60.00"), 33, 5, new BigDecimal("15.15")),
                new ComparativaProgramaDTO(UUID.randomUUID(), "Programa 4", false, 0, 0, 0, null, 0, 0, null)));

        mvc.perform(get("/api/v1/decano/comparativa-programas").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[0].nombrePrograma").value("Programa 1"))
                .andExpect(jsonPath("$.datos[0].porcentajeAusentismo").value(15.15))
                .andExpect(jsonPath("$.datos[0].estaActivoPrograma").value(true))
                .andExpect(jsonPath("$.datos[1].estaActivoPrograma").value(false))
                .andExpect(jsonPath("$.datos[1].porcentajeAusentismo").doesNotExist());

        final ArgumentCaptor<ConsultarComparativaProgramasDTO> captor = ArgumentCaptor.forClass(ConsultarComparativaProgramasDTO.class);
        verify(comparativa).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_comparativa_sin_facultad_responde_403() throws Exception {
        when(comparativa.execute(any())).thenThrow(new ForbiddenException("sin facultad"));

        mvc.perform(get("/api/v1/decano/comparativa-programas")).andExpect(status().isForbidden());
    }

    @Test
    void T_E2E_get_aforo_devuelve_la_pagina_con_total_y_delega_filtros() throws Exception {
        when(aforo.execute(any())).thenReturn(new AforoGruposPaginaDTO(List.of(
                new AforoGrupoDTO(GRUPO, 931001, UUID.randomUUID(), "Asignatura Uno", PROGRAMA, "Programa 1", 10, 5, 5, new BigDecimal("50.00"), true)), 12));

        mvc.perform(get("/api/v1/decano/aforo-grupos")
                        .param("pagina", "2").param("tamano", "5").param("idPrograma", PROGRAMA.toString())
                        .param("idPeriodoAcademico", PERIODO.toString()).param("soloHabilitados", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.datos[0].cupoMaximo").value(10))
                .andExpect(jsonPath("$.datos[0].tasaAforo").value(50.0))
                .andExpect(jsonPath("$.datos[0].grupoEstaHabilitado").value(true));

        final ArgumentCaptor<ConsultarAforoGruposDTO> captor = ArgumentCaptor.forClass(ConsultarAforoGruposDTO.class);
        verify(aforo).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals(PROGRAMA, captor.getValue().idPrograma());
        assertEquals(false, captor.getValue().soloHabilitados());
        assertEquals(2, captor.getValue().pagina());
        assertEquals(5, captor.getValue().tamano());
    }

    @Test
    void get_aforo_sin_parametros_delega_nulos_para_que_la_db_aplique_sus_defectos() throws Exception {
        when(aforo.execute(any())).thenReturn(new AforoGruposPaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/decano/aforo-grupos")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        final ArgumentCaptor<ConsultarAforoGruposDTO> captor = ArgumentCaptor.forClass(ConsultarAforoGruposDTO.class);
        verify(aforo).execute(captor.capture());
        assertNull(captor.getValue().pagina());
        assertNull(captor.getValue().tamano());
        assertNull(captor.getValue().soloHabilitados());
        assertNull(captor.getValue().idPrograma());
    }

    @Test
    void get_aforo_con_programa_ajeno_responde_404() throws Exception {
        when(aforo.execute(any())).thenThrow(new ResourceNotFoundException("El programa especificado no existe en el sistema."));

        mvc.perform(get("/api/v1/decano/aforo-grupos").param("idPrograma", PROGRAMA.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new DecanoIndicadoresController(null, comparativa, aforo, identity));
        assertThrows(NullPointerException.class, () -> new DecanoIndicadoresController(kpis, null, aforo, identity));
        assertThrows(NullPointerException.class, () -> new DecanoIndicadoresController(kpis, comparativa, null, identity));
        assertThrows(NullPointerException.class, () -> new DecanoIndicadoresController(kpis, comparativa, aforo, null));
    }
}
