package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.docente.consultarmatrizasistencia.primaryports.ConsultarMatrizAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.MatrizAsistenciaGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarresumensesiones.primaryports.ConsultarResumenSesionesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.common.dto.ResumenSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CIERRE-W3M: {@code GET /api/v1/docente/grupos/{grupoId}/matriz-asistencia} y {@code /resumen-sesiones}. */
class DocenteMatrizAsistenciaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();
    private static final UUID MATRICULA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final ConsultarMatrizAsistenciaGrupoInputPort matriz = mock(ConsultarMatrizAsistenciaGrupoInputPort.class);
    private final ConsultarResumenSesionesGrupoInputPort resumen = mock(ConsultarResumenSesionesGrupoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new DocenteMatrizAsistenciaController(matriz, resumen, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void la_matriz_devuelve_encabezado_filas_celdas_y_metadatos_de_pagina() throws Exception {
        when(matriz.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, 2, 10, "ana", ACTOR)))
                .thenReturn(new MatrizAsistenciaGrupoDTO(
                        List.of(new MatrizAsistenciaGrupoDTO.Sesion(SESION, 1, "Sesion 1", "EJECUTADA", Instant.parse("2026-11-10T13:00:00Z"),
                                Instant.parse("2026-11-10T15:00:00Z"), true)),
                        List.of(new MatrizAsistenciaGrupoDTO.Fila(MATRICULA, ESTUDIANTE, "Ana Perez", "100", "A",
                                List.of(new MatrizAsistenciaGrupoDTO.Celda(SESION, "PARCIAL", 60, 120)))), 2, 10, 25L));

        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO).param("pagina", "2").param("tamano", "10").param("busqueda", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.tamanoPagina").value(10))
                .andExpect(jsonPath("$.totalElementos").value(25))
                .andExpect(jsonPath("$.totalPaginas").value(3))
                .andExpect(jsonPath("$.sesiones[0].idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.sesiones[0].numero").value(1))
                .andExpect(jsonPath("$.sesiones[0].estadoSesion").value("EJECUTADA"))
                .andExpect(jsonPath("$.sesiones[0].fechaHoraInicioUtc").value("2026-11-10T13:00:00Z"))
                .andExpect(jsonPath("$.elementos[0].idEstudiante").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.elementos[0].nombreCompleto").value("Ana Perez"))
                .andExpect(jsonPath("$.elementos[0].celdas[0].idSesion").value(SESION.toString()))
                .andExpect(jsonPath("$.elementos[0].celdas[0].codigoEstado").value("PARCIAL"))
                .andExpect(jsonPath("$.elementos[0].celdas[0].minutosAsistidos").value(60))
                .andExpect(jsonPath("$.elementos[0].celdas[0].minutosSesion").value(120));
    }

    @Test
    void sin_parametros_pide_la_pagina_1_de_50_y_una_matriz_vacia_tiene_cero_paginas() throws Exception {
        when(matriz.execute(new ConsultarMatrizAsistenciaGrupoInputPort.Consulta(GRUPO, 1, 50, null, ACTOR)))
                .thenReturn(new MatrizAsistenciaGrupoDTO(List.of(), List.of(), 1, 50, 0L));

        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementos").isEmpty())
                .andExpect(jsonPath("$.sesiones").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0));
    }

    @Test
    void parametros_invalidos_son_400_y_no_llegan_al_caso_de_uso() throws Exception {
        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO).param("pagina", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO).param("tamano", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO).param("tamano", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO).param("busqueda", "x".repeat(101)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details[0].field").value("busqueda"));
        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", "no-es-uuid")).andExpect(status().isBadRequest());

        verify(matriz, never()).execute(any());
    }

    @Test
    void un_grupo_ajeno_es_403() throws Exception {
        when(matriz.execute(any())).thenThrow(new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_002));
        when(resumen.execute(any())).thenThrow(new ForbiddenException(MensajeUsuarioErrorCode.ACC_APP_002));

        mvc.perform(get("/api/v1/docente/grupos/{g}/matriz-asistencia", GRUPO)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/docente/grupos/{g}/resumen-sesiones", GRUPO)).andExpect(status().isForbidden());
    }

    @Test
    void el_resumen_de_sesiones_entrega_estado_temporal_conteos_e_indicadores() throws Exception {
        when(resumen.execute(new ConsultarResumenSesionesGrupoInputPort.Consulta(GRUPO, ACTOR))).thenReturn(new ResumenSesionesGrupoDTO(
                3, 1, 1, 1, SESION, null, 30, 4, new BigDecimal("87.50"), new BigDecimal("15.00"), new BigDecimal("20.00"), List.of(
                new ResumenSesionesGrupoDTO.Sesion(SESION, 2, "S2", Instant.parse("2026-11-16T13:00:00Z"), Instant.parse("2026-11-16T15:00:00Z"),
                        true, "EN_CURSO", null, false, 30, 20, 1, 2, 3, 4))));

        mvc.perform(get("/api/v1/docente/grupos/{g}/resumen-sesiones", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.sesionesProgramadas").value(3))
                .andExpect(jsonPath("$.datos.sesionesEjecutadas").value(1))
                .andExpect(jsonPath("$.datos.sesionesCanceladas").value(1))
                .andExpect(jsonPath("$.datos.sesionesEnCurso").value(1))
                .andExpect(jsonPath("$.datos.idSesionEnCurso").value(SESION.toString()))
                .andExpect(jsonPath("$.datos.idSesionProxima").isEmpty())
                .andExpect(jsonPath("$.datos.estudiantesEnRiesgo").value(4))
                .andExpect(jsonPath("$.datos.promedioAsistencia").value(87.50))
                .andExpect(jsonPath("$.datos.umbralAdvertencia").value(15.00))
                .andExpect(jsonPath("$.datos.sesiones[0].estadoSesion").value("EN_CURSO"))
                .andExpect(jsonPath("$.datos.sesiones[0].presentes").value(20))
                .andExpect(jsonPath("$.datos.sesiones[0].sinRegistro").value(4))
                .andExpect(jsonPath("$.datos.sesiones[0].horarioConfirmado").value(true));
    }
}
