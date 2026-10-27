package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

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
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.ConsultarComparativaFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ComparativaFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcomparativafacultades.primaryports.dto.ConsultarComparativaFacultadesDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.ConsultarKpisInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.ConsultarKpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.primaryports.dto.KpisInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.ConsultarTendenciaAusentismoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.ConsultarTendenciaAusentismoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.primaryports.dto.PuntoTendenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminIndicadoresControllerTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUCION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID FACULTAD = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final ConsultarKpisInstitucionInputPort kpis = mock(ConsultarKpisInstitucionInputPort.class);
    private final ConsultarComparativaFacultadesInputPort comparativa = mock(ConsultarComparativaFacultadesInputPort.class);
    private final ConsultarTendenciaAusentismoInputPort tendencia = mock(ConsultarTendenciaAusentismoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminIndicadoresController(kpis, comparativa, tendencia, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> "KPI_004".equals(codigo)
                    ? Optional.of("No tiene una institucion a su cargo para consultar estos indicadores.") : Optional.empty())).build();

    @Test
    void T_E2E_get_indicadores_serializa_cada_cifra_y_conserva_los_nulos() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisInstitucionDTO(INSTITUCION, "Universidad", PERIODO, "2026-2", 3, 4, 6, 2, 1, 3, new BigDecimal("50.00"),
                35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 12, 11, null, 6, 7, 1, 2, new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/admin/indicadores").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.totalFacultades").value(3))
                .andExpect(jsonPath("$.datos.totalProgramas").value(4))
                .andExpect(jsonPath("$.datos.tasaRetencion").value(50.0))
                .andExpect(jsonPath("$.datos.porcentajeAusentismo").value(17.14))
                .andExpect(jsonPath("$.datos.porcentajeAsistencia").value(82.86))
                .andExpect(jsonPath("$.datos.sesionesVencidas").value(12))
                .andExpect(jsonPath("$.datos.sesionesConRegistro").value(11))
                .andExpect(jsonPath("$.datos.porcentajeCumplimiento").doesNotExist())
                .andExpect(jsonPath("$.datos.estudiantesConMatriculaActiva").value(6))
                .andExpect(jsonPath("$.datos.estudiantesRegistrados").value(7))
                .andExpect(jsonPath("$.datos.docentesConGrupoActivo").value(1))
                .andExpect(jsonPath("$.datos.docentesRegistrados").value(2))
                .andExpect(jsonPath("$.datos.umbralPerdida").value(20.0));

        final ArgumentCaptor<ConsultarKpisInstitucionDTO> captor = ArgumentCaptor.forClass(ConsultarKpisInstitucionDTO.class);
        verify(kpis).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_indicadores_sin_periodo_delega_nulo() throws Exception {
        when(kpis.execute(any())).thenReturn(new KpisInstitucionDTO(INSTITUCION, "Universidad", null, null, 1, 1, 0, 0, 0, 0, null, 0, 0, null, null,
                0, 0, null, 0, 0, 0, 0, new BigDecimal("15.00"), new BigDecimal("20.00")));

        mvc.perform(get("/api/v1/admin/indicadores")).andExpect(status().isOk());

        final ArgumentCaptor<ConsultarKpisInstitucionDTO> captor = ArgumentCaptor.forClass(ConsultarKpisInstitucionDTO.class);
        verify(kpis).execute(captor.capture());
        assertNull(captor.getValue().idPeriodoAcademico());
    }

    @Test
    void T_SEC_get_indicadores_sin_institucion_responde_403_con_el_mensaje_del_catalogo() throws Exception {
        when(kpis.execute(any())).thenThrow(new ForbiddenException("KPI_004", "Ambito: el ejecutor no es administrador."));

        mvc.perform(get("/api/v1/admin/indicadores"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene una institucion a su cargo para consultar estos indicadores."));
    }

    @Test
    void T_E2E_get_comparativa_devuelve_una_fila_por_facultad_con_nulos() throws Exception {
        when(comparativa.execute(any())).thenReturn(List.of(
                new ComparativaFacultadDTO(FACULTAD, "Ingenieria", true, 3, 6, 1, 3, new BigDecimal("50.00"), 35, 6, new BigDecimal("17.14")),
                new ComparativaFacultadDTO(UUID.randomUUID(), "Cerrada", false, 0, 0, 0, 0, null, 0, 0, null)));

        mvc.perform(get("/api/v1/admin/comparativa-facultades").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[0].nombreFacultad").value("Ingenieria"))
                .andExpect(jsonPath("$.datos[0].porcentajeAusentismo").value(17.14))
                .andExpect(jsonPath("$.datos[0].estaActivaFacultad").value(true))
                .andExpect(jsonPath("$.datos[1].estaActivaFacultad").value(false))
                .andExpect(jsonPath("$.datos[1].porcentajeAusentismo").doesNotExist());

        final ArgumentCaptor<ConsultarComparativaFacultadesDTO> captor = ArgumentCaptor.forClass(ConsultarComparativaFacultadesDTO.class);
        verify(comparativa).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_comparativa_con_periodo_inexistente_responde_404() throws Exception {
        when(comparativa.execute(any())).thenThrow(new ResourceNotFoundException("periodo"));

        mvc.perform(get("/api/v1/admin/comparativa-facultades")).andExpect(status().isNotFound());
    }

    @Test
    void T_E2E_get_tendencia_devuelve_los_puntos_con_fechas_iso_y_delega_la_granularidad() throws Exception {
        when(tendencia.execute(any())).thenReturn(List.of(
                new PuntoTendenciaDTO(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), 20, 3, new BigDecimal("15.00")),
                new PuntoTendenciaDTO(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 15), 0, 0, null)));

        mvc.perform(get("/api/v1/admin/tendencia-ausentismo").param("granularidad", "MES").param("idPeriodoAcademico", PERIODO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[0].inicioIntervalo").value("2026-03-02"))
                .andExpect(jsonPath("$.datos[0].finIntervalo").value("2026-03-08"))
                .andExpect(jsonPath("$.datos[0].sesionesEjecutadas").value(20))
                .andExpect(jsonPath("$.datos[0].porcentajeAusentismo").value(15.0))
                .andExpect(jsonPath("$.datos[1].porcentajeAusentismo").doesNotExist());

        final ArgumentCaptor<ConsultarTendenciaAusentismoDTO> captor = ArgumentCaptor.forClass(ConsultarTendenciaAusentismoDTO.class);
        verify(tendencia).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals("MES", captor.getValue().granularidad());
    }

    @Test
    void get_tendencia_sin_parametros_delega_nulos_para_que_el_caso_de_uso_aplique_el_defecto() throws Exception {
        when(tendencia.execute(any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/admin/tendencia-ausentismo")).andExpect(status().isOk()).andExpect(jsonPath("$.datos.length()").value(0));

        final ArgumentCaptor<ConsultarTendenciaAusentismoDTO> captor = ArgumentCaptor.forClass(ConsultarTendenciaAusentismoDTO.class);
        verify(tendencia).execute(captor.capture());
        assertNull(captor.getValue().granularidad());
        assertNull(captor.getValue().idPeriodoAcademico());
    }

    @Test
    void get_tendencia_con_granularidad_invalida_responde_400() throws Exception {
        when(tendencia.execute(any())).thenThrow(new ValidationException("La granularidad debe ser SEMANA o MES."));

        mvc.perform(get("/api/v1/admin/tendencia-ausentismo").param("granularidad", "ANIO")).andExpect(status().isBadRequest());
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new AdminIndicadoresController(null, comparativa, tendencia, identity));
        assertThrows(NullPointerException.class, () -> new AdminIndicadoresController(kpis, null, tendencia, identity));
        assertThrows(NullPointerException.class, () -> new AdminIndicadoresController(kpis, comparativa, null, identity));
        assertThrows(NullPointerException.class, () -> new AdminIndicadoresController(kpis, comparativa, tendencia, null));
    }
}
