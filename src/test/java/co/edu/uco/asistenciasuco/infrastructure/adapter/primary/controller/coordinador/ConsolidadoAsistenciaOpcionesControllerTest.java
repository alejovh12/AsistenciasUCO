package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.OpcionFiltroDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.OpcionesConsolidadoDTO;
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

/** CIERRE-W2A (punto 3): {@code GET /{coordinador,decano}/consolidado-asistencia/opciones}. */
class ConsolidadoAsistenciaOpcionesControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID ASIGNATURA = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();

    private final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ConsolidadoAsistenciaController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void las_dos_rutas_entregan_asignaturas_y_docentes_del_periodo() throws Exception {
        when(inputPort.consultarOpcionesConsolidado(ACTOR, PERIODO)).thenReturn(new OpcionesConsolidadoDTO(
                List.of(new OpcionFiltroDTO(ASIGNATURA, "Calculo")), List.of(new OpcionFiltroDTO(DOCENTE, "Ana Perez"))));

        for (final String ruta : List.of("/api/v1/coordinador/consolidado-asistencia/opciones",
                "/api/v1/decano/consolidado-asistencia/opciones")) {
            mvc.perform(get(ruta).param("idPeriodoAcademico", PERIODO.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitoso").value(true))
                    .andExpect(jsonPath("$.datos.asignaturas[0].id").value(ASIGNATURA.toString()))
                    .andExpect(jsonPath("$.datos.asignaturas[0].nombre").value("Calculo"))
                    .andExpect(jsonPath("$.datos.docentes[0].id").value(DOCENTE.toString()))
                    .andExpect(jsonPath("$.datos.docentes[0].nombre").value("Ana Perez"));
        }
    }

    @Test
    void sin_periodo_pide_todos_y_una_lista_vacia_es_valida() throws Exception {
        when(inputPort.consultarOpcionesConsolidado(ACTOR, null))
                .thenReturn(new OpcionesConsolidadoDTO(List.of(), List.of()));

        mvc.perform(get("/api/v1/coordinador/consolidado-asistencia/opciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.asignaturas").isEmpty())
                .andExpect(jsonPath("$.datos.docentes").isEmpty());
        verify(inputPort).consultarOpcionesConsolidado(ACTOR, null);
    }

    @Test
    void un_periodo_que_no_es_uuid_es_400() throws Exception {
        mvc.perform(get("/api/v1/decano/consolidado-asistencia/opciones").param("idPeriodoAcademico", "no-es-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void un_perfil_sin_ambito_es_403() throws Exception {
        when(inputPort.consultarOpcionesConsolidado(ACTOR, null)).thenThrow(new ForbiddenException("SEC_001"));

        mvc.perform(get("/api/v1/coordinador/consolidado-asistencia/opciones")).andExpect(status().isForbidden());
    }
}
