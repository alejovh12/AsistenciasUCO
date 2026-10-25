package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.ConsultarAlertaTempranaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.AlertasPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.primaryports.dto.ConsultarAlertaTempranaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.ConsultarBitacoraEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.BitacoraSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.primaryports.dto.ConsultarBitacoraEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.ConsultarFichaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ConsultarFichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto.ResumenAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.ConsultarKpisCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.ConsultarKpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.primaryports.dto.KpisCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoordinadorIndicadoresControllerTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROGRAMA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ESTUDIANTE = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final ConsultarKpisCoordinadorInputPort kpis = mock(ConsultarKpisCoordinadorInputPort.class);
    private final ConsultarAlertaTempranaInputPort alertas = mock(ConsultarAlertaTempranaInputPort.class);
    private final ConsultarBitacoraEstudianteInputPort bitacora = mock(ConsultarBitacoraEstudianteInputPort.class);
    private final ConsultarFichaEstudianteInputPort ficha = mock(ConsultarFichaEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(
                    new CoordinadorIndicadoresController(kpis, alertas, identity),
                    new CoordinadorFichaEstudianteController(bitacora, ficha, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_KPI_01_get_indicadores_serializa_cada_cifra_y_conserva_los_nulos() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisCoordinadorDTO(PROGRAMA, "Sistemas", PERIODO, "2026-2", 5, 2, 1, 2, new BigDecimal("60.00"),
                15, 10, 9, 1, 1, null, new BigDecimal("66.67"), new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/coordinador/indicadores").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.totalEstudiantes").value(5))
                .andExpect(jsonPath("$.datos.estudiantesPerdida").value(2))
                .andExpect(jsonPath("$.datos.tasaPermanencia").value(60.0))
                .andExpect(jsonPath("$.datos.sesionesEjecutadas").value(9))
                .andExpect(jsonPath("$.datos.porcentajeCumplimiento").doesNotExist())
                .andExpect(jsonPath("$.datos.porcentajeProgresoCalendario").value(66.67))
                .andExpect(jsonPath("$.datos.umbralPerdida").value(20.0));

        final ArgumentCaptor<ConsultarKpisCoordinadorDTO> captor = ArgumentCaptor.forClass(ConsultarKpisCoordinadorDTO.class);
        verify(kpis).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_indicadores_sin_periodo_delega_nulo() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisCoordinadorDTO(PROGRAMA, "Sistemas", null, null, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, null, null,
                new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/coordinador/indicadores")).andExpect(status().isOk());

        final ArgumentCaptor<ConsultarKpisCoordinadorDTO> captor = ArgumentCaptor.forClass(ConsultarKpisCoordinadorDTO.class);
        verify(kpis).execute(captor.capture());
        assertNull(captor.getValue().idPeriodoAcademico());
    }

    @Test
    void T_SEC_05_indicadores_sin_programa_responde_403() throws Exception {
        when(kpis.execute(any())).thenThrow(new ForbiddenException("sin programa"));

        mvc.perform(get("/api/v1/coordinador/indicadores")).andExpect(status().isForbidden());
    }

    @Test
    void T_ALT_01_get_alertas_devuelve_la_pagina_con_total() throws Exception {
        when(alertas.execute(any())).thenReturn(new AlertasPaginaDTO(List.of(new AlertaTempranaDTO(ESTUDIANTE, "Ana Perez", 1017000001, "ana@uco.edu.co",
                2, new BigDecimal("66.67"), 2, "PERDIDA", "ROJO")), 12));

        mvc.perform(get("/api/v1/coordinador/alertas-tempranas").param("pagina", "2").param("tamano", "5")
                        .param("semaforo", "ROJO").param("nombre", "Ana").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.datos[0].idEstudiante").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.datos[0].porcentajeInasistencia").value(66.67))
                .andExpect(jsonPath("$.datos[0].semaforo").value("ROJO"));

        final ArgumentCaptor<ConsultarAlertaTempranaDTO> captor = ArgumentCaptor.forClass(ConsultarAlertaTempranaDTO.class);
        verify(alertas).execute(captor.capture());
        assertEquals(2, captor.getValue().pagina());
        assertEquals(5, captor.getValue().tamano());
        assertEquals("ROJO", captor.getValue().semaforo());
        assertEquals("Ana", captor.getValue().nombre());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_alertas_sin_parametros_delega_nulos_para_que_el_caso_de_uso_aplique_los_defectos() throws Exception {
        when(alertas.execute(any())).thenReturn(new AlertasPaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/coordinador/alertas-tempranas")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        final ArgumentCaptor<ConsultarAlertaTempranaDTO> captor = ArgumentCaptor.forClass(ConsultarAlertaTempranaDTO.class);
        verify(alertas).execute(captor.capture());
        assertNull(captor.getValue().pagina());
        assertNull(captor.getValue().tamano());
    }

    @Test
    void T_BIT_01_get_bitacora_serializa_asistio_nulo_y_el_total() throws Exception {
        when(bitacora.execute(any())).thenReturn(new BitacoraPaginaDTO(List.of(new BitacoraSesionDTO(UUID.randomUUID(), 7, "Sesion G1-7",
                Instant.parse("2026-10-23T14:00:00Z"), Instant.parse("2026-10-23T16:00:00Z"), GRUPO, 931001, UUID.randomUUID(), "Estructuras", null, null, null)), 25));

        mvc.perform(get("/api/v1/coordinador/estudiantes/" + ESTUDIANTE + "/bitacora").param("pagina", "3").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.datos[0].numeroSesion").value(7))
                .andExpect(jsonPath("$.datos[0].asistio").doesNotExist())
                .andExpect(jsonPath("$.datos[0].fechaHoraInicio").value("2026-10-23T14:00:00Z"));

        final ArgumentCaptor<ConsultarBitacoraEstudianteDTO> captor = ArgumentCaptor.forClass(ConsultarBitacoraEstudianteDTO.class);
        verify(bitacora).execute(captor.capture());
        assertEquals(ESTUDIANTE, captor.getValue().idEstudiante());
        assertEquals(3, captor.getValue().pagina());
    }

    @Test
    void T_FIC_03_estudiante_ajeno_o_inexistente_responde_404() throws Exception {
        when(bitacora.execute(any())).thenThrow(new ResourceNotFoundException("no existe"));
        when(ficha.execute(any())).thenThrow(new ResourceNotFoundException("no existe"));

        mvc.perform(get("/api/v1/coordinador/estudiantes/" + ESTUDIANTE + "/bitacora")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/coordinador/estudiantes/" + ESTUDIANTE + "/ficha")).andExpect(status().isNotFound());
    }

    @Test
    void T_FIC_01_get_ficha_serializa_cabecera_y_resumen_sin_telefono() throws Exception {
        when(ficha.execute(any())).thenReturn(new FichaEstudianteDTO(ESTUDIANTE, "CC", 1017000001, "Ana", "Maria", "Perez", "Gomez", "Ana Maria Perez Gomez",
                "ana@uco.edu.co", true, PROGRAMA, "Sistemas",
                List.of(new ResumenAsignaturaDTO(GRUPO, 931001, UUID.randomUUID(), "Estructuras", "A", "activo", 6, 4, 2, 0, new BigDecimal("33.33"), "PERDIDA"))));

        mvc.perform(get("/api/v1/coordinador/estudiantes/" + ESTUDIANTE + "/ficha").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.correo").value("ana@uco.edu.co"))
                .andExpect(jsonPath("$.datos.telefono").doesNotExist())
                .andExpect(jsonPath("$.datos.resumenPorAsignatura[0].faltasSinJustaCausa").value(2))
                .andExpect(jsonPath("$.datos.resumenPorAsignatura[0].porcentajeInasistencia").value(33.33))
                .andExpect(jsonPath("$.datos.resumenPorAsignatura[0].codigoPermanencia").value("PERDIDA"));

        final ArgumentCaptor<ConsultarFichaEstudianteDTO> captor = ArgumentCaptor.forClass(ConsultarFichaEstudianteDTO.class);
        verify(ficha).execute(captor.capture());
        assertEquals(ESTUDIANTE, captor.getValue().idEstudiante());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void un_id_de_estudiante_que_no_es_uuid_responde_400_sin_llamar_al_caso_de_uso() throws Exception {
        mvc.perform(get("/api/v1/coordinador/estudiantes/no-es-uuid/ficha")).andExpect(status().isBadRequest());

        verify(ficha, never()).execute(any());
    }
}
